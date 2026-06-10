/**
 * Gestor de Negócio SaaS — app.js
 * Utilitários globais e interações com HTMX
 */

/* ============================================================
   Inicialização global
   ============================================================ */
document.addEventListener('DOMContentLoaded', function () {

    // Fechar alertas automaticamente após 5 segundos
    document.querySelectorAll('.alert.alert-dismissible').forEach(function (alert) {
        setTimeout(function () {
            const btn = alert.querySelector('.btn-close');
            if (btn) btn.click();
        }, 5000);
    });

    // Inicializar tooltips Bootstrap
    const tooltips = document.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltips.forEach(t => new bootstrap.Tooltip(t));

    // Inicializar popovers Bootstrap
    const popovers = document.querySelectorAll('[data-bs-toggle="popover"]');
    popovers.forEach(p => new bootstrap.Popover(p));

    // Confirmar ações de exclusão
    document.querySelectorAll('[data-confirm]').forEach(function (el) {
        el.addEventListener('click', function (e) {
            const msg = el.dataset.confirm || 'Tem certeza que deseja excluir este item?';
            if (!confirm(msg)) {
                e.preventDefault();
                e.stopPropagation();
            }
        });
    });
});

/* ============================================================
   HTMX helpers
   ============================================================ */
document.addEventListener('htmx:afterRequest', function (evt) {
    // Fechar modais Bootstrap após requests HTMX bem-sucedidos
    if (evt.detail.xhr.status >= 200 && evt.detail.xhr.status < 300) {
        const openModal = document.querySelector('.modal.show');
        if (openModal) {
            const modal = bootstrap.Modal.getInstance(openModal);
            if (modal) modal.hide();
        }
    }
});

document.addEventListener('htmx:responseError', function (evt) {
    console.error('HTMX Error:', evt.detail.xhr.status, evt.detail.xhr.responseText);
});

/* ============================================================
   Utilitários
   ============================================================ */

/**
 * Mostrar/ocultar campo de senha
 */
function togglePassword(fieldId, btn) {
    const field = document.getElementById(fieldId);
    const icon = btn.querySelector('i');
    if (field.type === 'password') {
        field.type = 'text';
        icon.className = 'bi bi-eye-slash';
    } else {
        field.type = 'password';
        icon.className = 'bi bi-eye';
    }
}

/**
 * Formatar moeda em BRL
 */
function formatBRL(value) {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(value);
}

/**
 * PDV: gerenciar carrinho de compras no frontend
 */
const Cart = {
    items: {},

    addItem(productId, productName, price) {
        if (this.items[productId]) {
            this.items[productId].quantity++;
        } else {
            this.items[productId] = { name: productName, price: parseFloat(price), quantity: 1 };
        }
        this.render();
    },

    removeItem(productId) {
        delete this.items[productId];
        this.render();
    },

    incrementItem(productId) {
        if (this.items[productId]) this.items[productId].quantity++;
        this.render();
    },

    decrementItem(productId) {
        if (this.items[productId]) {
            this.items[productId].quantity--;
            if (this.items[productId].quantity <= 0) this.removeItem(productId);
        }
        this.render();
    },

    total() {
        return Object.values(this.items).reduce((sum, item) => sum + item.price * item.quantity, 0);
    },

    render() {
        const container = document.getElementById('cartItems');
        const totalEl = document.getElementById('cartTotal');
        const countEl = document.getElementById('cartCount');
        const submitBtn = document.getElementById('finalizeSaleBtn');
        const form = document.getElementById('saleForm');

        if (!container) return;

        const keys = Object.keys(this.items);
        container.innerHTML = '';

        // Limpar inputs ocultos existentes
        if (form) {
            form.querySelectorAll('input[name="productId"], input[name="quantity"]').forEach(e => e.remove());
        }

        if (keys.length === 0) {
            container.innerHTML = '<p class="text-muted text-center py-3 mb-0">Carrinho vazio</p>';
            if (submitBtn) submitBtn.disabled = true;
            if (totalEl) totalEl.textContent = formatBRL(0);
            if (countEl) countEl.textContent = '0';
            return;
        }

        keys.forEach(productId => {
            const item = this.items[productId];
            const li = document.createElement('div');
            li.className = 'cart-item d-flex align-items-center justify-content-between mb-2 p-2 rounded bg-light';
            li.innerHTML = `
                <div class="flex-grow-1 me-2">
                    <div class="fw-semibold small">${item.name}</div>
                    <div class="text-muted small">${formatBRL(item.price)} × ${item.quantity}</div>
                </div>
                <div class="d-flex align-items-center gap-1">
                    <button type="button" class="btn btn-sm btn-outline-secondary py-0 px-1" onclick="Cart.decrementItem('${productId}')">−</button>
                    <span class="small fw-bold mx-1">${item.quantity}</span>
                    <button type="button" class="btn btn-sm btn-outline-secondary py-0 px-1" onclick="Cart.incrementItem('${productId}')">+</button>
                    <button type="button" class="btn btn-sm btn-outline-danger py-0 px-1 ms-1" onclick="Cart.removeItem('${productId}')"><i class="bi bi-x"></i></button>
                </div>
                <div class="ms-2 fw-bold text-primary small">${formatBRL(item.price * item.quantity)}</div>
            `;
            container.appendChild(li);

            // Adicionar inputs ocultos ao formulário
            if (form) {
                const pidInput = document.createElement('input');
                pidInput.type = 'hidden';
                pidInput.name = 'productId';
                pidInput.value = productId;
                form.appendChild(pidInput);

                const qtyInput = document.createElement('input');
                qtyInput.type = 'hidden';
                qtyInput.name = 'quantity';
                qtyInput.value = item.quantity;
                form.appendChild(qtyInput);
            }
        });

        if (totalEl) totalEl.textContent = formatBRL(this.total());
        if (countEl) countEl.textContent = keys.length;
        if (submitBtn) submitBtn.disabled = false;
    }
};
