"""
seed_demo_chat.py — Popular o banco com dados fictícios ricos para demonstrar
o Chatbot (Copiloto de Negócios) com respostas completas em todos os intents.

O chatbot consulta diretamente as tabelas do banco. Este script garante que
TODOS os intents retornem dados significativos:

  📊 Faturamento  → 47 pedidos com total ~R$ 5.800
  ⚠️ Estoque      → 3 insumos em ruptura crítica + 9 saudáveis
  📅 Escalas      → 4 colaboradores escalados para HOJE
  🤝 Fornecedores → 5 parceiros comerciais com contatos

Uso:
  DATABASE_URL="sqlite+aiosqlite:///./gestor.db" \
  JWT_SECRET="demo-secret-key" DB_USER=x DB_PASSWORD=x \
  python seed_demo_chat.py
"""

import asyncio
import random
from datetime import date, datetime, timedelta, timezone

from app.core.database import SessionLocal, engine, Base
from app.core.security import get_password_hash
from app.models.tenant import Tenant
from app.models.user import User
from app.models.category import Category
from app.models.insumo import Insumo
from app.models.product import Product, ProductIngredient
from app.models.supplier import Supplier
from app.models.order import Order, OrderItem
from app.models.schedules import EmployeeSchedule
from app.models.purchase import PurchaseOrder, PurchaseItem


async def seed_demo():
    print("🔄 Recriando banco de dados completo para demonstração do Chatbot...\n")

    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.drop_all)
        await conn.run_sync(Base.metadata.create_all)

    async with SessionLocal() as db:
        # ════════════════════════════════════════════════════════════════════
        # 1. TENANT (food_service para habilitar todos os módulos do chat)
        # ════════════════════════════════════════════════════════════════════
        tenant = Tenant(
            name="Sabores & Cia Ltda",
            slug="sabores-cia",
            sector_type="food_service",
        )
        db.add(tenant)
        await db.flush()
        t_id = tenant.id
        print(f"🏢 Tenant: {tenant.name} (setor: food_service)")

        # ════════════════════════════════════════════════════════════════════
        # 2. USUÁRIOS (5 colaboradores com funções variadas)
        # ════════════════════════════════════════════════════════════════════
        users_data = [
            ("João Silva", "joao@saborescia.com", "admin123", "OWNER"),
            ("Ana Beatriz Costa", "ana@saborescia.com", "senha123", "MANAGER"),
            ("Carlos Eduardo Lima", "carlos@saborescia.com", "senha123", "SUPERVISOR"),
            ("Maria Santos", "maria@saborescia.com", "senha123", "OPERATOR"),
            ("Pedro Henrique Oliveira", "pedro@saborescia.com", "senha123", "OPERATOR"),
        ]

        users = []
        for name, email, pwd, role in users_data:
            u = User(
                tenant_id=t_id,
                name=name,
                email=email,
                hashed_password=get_password_hash(pwd),
                role=role,
                is_active=True,
            )
            db.add(u)
            users.append(u)

        await db.flush()
        print(f"👥 {len(users)} usuários criados")

        # ════════════════════════════════════════════════════════════════════
        # 3. CATEGORIAS (insumos + produtos)
        # ════════════════════════════════════════════════════════════════════
        cat_carnes = Category(tenant_id=t_id, name="Carnes e Frios", type="INSUMO")
        cat_horti = Category(tenant_id=t_id, name="Hortifruti", type="INSUMO")
        cat_merc = Category(tenant_id=t_id, name="Mercearia e Secos", type="INSUMO")
        cat_bebidas_i = Category(tenant_id=t_id, name="Bebidas (Insumo)", type="INSUMO")
        cat_embalagens = Category(tenant_id=t_id, name="Embalagens", type="INSUMO")

        cat_lanches = Category(tenant_id=t_id, name="Lanches", type="PRODUCT")
        cat_bebidas_p = Category(tenant_id=t_id, name="Bebidas", type="PRODUCT")
        cat_pratos = Category(tenant_id=t_id, name="Pratos Executivos", type="PRODUCT")
        cat_sobremesas = Category(tenant_id=t_id, name="Sobremesas", type="PRODUCT")

        all_cats = [
            cat_carnes, cat_horti, cat_merc, cat_bebidas_i, cat_embalagens,
            cat_lanches, cat_bebidas_p, cat_pratos, cat_sobremesas,
        ]
        db.add_all(all_cats)
        await db.flush()
        print(f"📂 {len(all_cats)} categorias criadas")

        # ════════════════════════════════════════════════════════════════════
        # 4. INSUMOS (12 itens — 3 com estoque CRÍTICO para alertas do chat)
        # ════════════════════════════════════════════════════════════════════
        insumos_data = [
            # (nome, unidade, estoque_atual, estoque_minimo, custo, categoria)
            # --- SAUDÁVEIS (acima do mínimo) ---
            ("Pão de Hambúrguer Brioche", "un", 120.0, 50.0, 1.50, cat_merc),
            ("Blend de Carne Angus 180g", "kg", 22.0, 10.0, 38.00, cat_carnes),
            ("Alface Americana", "un", 35.0, 10.0, 2.80, cat_horti),
            ("Tomate Carmem", "kg", 18.0, 5.0, 7.20, cat_horti),
            ("Batata Frita Congelada", "kg", 40.0, 15.0, 14.90, cat_merc),
            ("Coca-Cola Lata 350ml", "un", 200.0, 48.0, 2.80, cat_bebidas_i),
            ("Caixa Kraft para Burger", "un", 400.0, 100.0, 0.45, cat_embalagens),
            ("Arroz Agulhinha Tipo 1", "kg", 50.0, 10.0, 5.60, cat_merc),
            ("Feijão Carioca", "kg", 30.0, 8.0, 7.90, cat_merc),
            # --- CRÍTICOS (abaixo do mínimo — alertas do chatbot) ---
            ("Queijo Cheddar Fatiado", "kg", 1.2, 5.0, 42.00, cat_carnes),
            ("Óleo de Soja 5L", "un", 1.0, 4.0, 22.50, cat_merc),
            ("Bacon em Fatias", "kg", 0.8, 3.0, 48.00, cat_carnes),
        ]

        insumos = []
        for name, unit, stock, min_stock, cost, cat in insumos_data:
            ins = Insumo(
                tenant_id=t_id,
                category_id=cat.id,
                name=name,
                unit=unit,
                current_stock=stock,
                minimum_stock=min_stock,
                unit_cost=cost,
            )
            db.add(ins)
            insumos.append(ins)

        await db.flush()
        n_critical = sum(1 for i in insumos if i.current_stock < i.minimum_stock)
        print(f"📦 {len(insumos)} insumos criados ({n_critical} em ruptura crítica ⚠️)")

        # ════════════════════════════════════════════════════════════════════
        # 5. PRODUTOS com Ficha Técnica (receitas de ingredientes)
        # ════════════════════════════════════════════════════════════════════
        products_data = [
            ("Cheeseburger Clássico", 28.90, cat_lanches),
            ("Cheeseburger Duplo Bacon", 38.90, cat_lanches),
            ("Porção de Batatas (Média)", 16.90, cat_lanches),
            ("Porção de Batatas (Grande)", 24.90, cat_lanches),
            ("Refrigerante Lata 350ml", 6.00, cat_bebidas_p),
            ("Suco Natural 500ml", 9.90, cat_bebidas_p),
            ("Prato Executivo — Arroz, Feijão e Carne", 22.90, cat_pratos),
            ("Brownie com Sorvete", 14.90, cat_sobremesas),
        ]

        products = []
        for name, price, cat in products_data:
            p = Product(
                tenant_id=t_id,
                category_id=cat.id,
                name=name,
                price=price,
                is_active=True,
            )
            db.add(p)
            products.append(p)

        await db.flush()

        # Fichas técnicas (receitas) para os primeiros produtos
        # Cheeseburger Clássico: pão + carne + queijo + alface + tomate + caixa
        db.add_all([
            ProductIngredient(product_id=products[0].id, insumo_id=insumos[0].id, quantity=1.0),
            ProductIngredient(product_id=products[0].id, insumo_id=insumos[1].id, quantity=0.180),
            ProductIngredient(product_id=products[0].id, insumo_id=insumos[9].id, quantity=0.040),
            ProductIngredient(product_id=products[0].id, insumo_id=insumos[2].id, quantity=0.10),
            ProductIngredient(product_id=products[0].id, insumo_id=insumos[3].id, quantity=0.050),
            ProductIngredient(product_id=products[0].id, insumo_id=insumos[6].id, quantity=1.0),
        ])
        # Cheeseburger Duplo Bacon: pão + 2x carne + queijo + bacon + caixa
        db.add_all([
            ProductIngredient(product_id=products[1].id, insumo_id=insumos[0].id, quantity=1.0),
            ProductIngredient(product_id=products[1].id, insumo_id=insumos[1].id, quantity=0.360),
            ProductIngredient(product_id=products[1].id, insumo_id=insumos[9].id, quantity=0.060),
            ProductIngredient(product_id=products[1].id, insumo_id=insumos[11].id, quantity=0.080),
            ProductIngredient(product_id=products[1].id, insumo_id=insumos[6].id, quantity=1.0),
        ])
        # Porção de Batatas Média
        db.add(ProductIngredient(product_id=products[2].id, insumo_id=insumos[4].id, quantity=0.250))
        # Porção de Batatas Grande
        db.add(ProductIngredient(product_id=products[3].id, insumo_id=insumos[4].id, quantity=0.400))
        # Refrigerante
        db.add(ProductIngredient(product_id=products[4].id, insumo_id=insumos[5].id, quantity=1.0))
        # Prato Executivo
        db.add_all([
            ProductIngredient(product_id=products[6].id, insumo_id=insumos[7].id, quantity=0.150),
            ProductIngredient(product_id=products[6].id, insumo_id=insumos[8].id, quantity=0.100),
            ProductIngredient(product_id=products[6].id, insumo_id=insumos[1].id, quantity=0.200),
        ])
        await db.flush()
        print(f"🍔 {len(products)} produtos com fichas técnicas criados")

        # ════════════════════════════════════════════════════════════════════
        # 6. FORNECEDORES (5 parceiros comerciais com contatos completos)
        # ════════════════════════════════════════════════════════════════════
        suppliers_data = [
            ("Frigorífico Boi Gordo S/A", "Carlos Mendes", "vendas@boigordo.com.br", "(83) 9988-7766", "12345678000190"),
            ("Hortifruti Frescor da Fazenda", "Dona Ana", "contato@frescorfazenda.com.br", "(83) 3344-5566", "98765432000155"),
            ("Distribuidora de Bebidas Geladão", "Beto Souza", "pedidos@geladao.com", "(83) 9777-6655", "11222333000144"),
            ("Empório de Especiarias Nordeste", "Seu Raimundo", "raimundo@emporio.com.br", "(83) 9876-1234", "55667788000177"),
            ("Pães & Grãos Artesanais Ltda", "Luciana Matos", "luciana@paesgraos.com.br", "(83) 9911-2233", "44332211000188"),
        ]

        suppliers = []
        for name, contact, email, phone, doc in suppliers_data:
            s = Supplier(
                tenant_id=t_id,
                name=name,
                contact_name=contact,
                email=email,
                phone=phone,
                document=doc,
            )
            db.add(s)
            suppliers.append(s)

        await db.flush()
        print(f"🤝 {len(suppliers)} fornecedores cadastrados")

        # ════════════════════════════════════════════════════════════════════
        # 7. PEDIDOS DE VENDA (47 pedidos nos últimos 14 dias)
        #    Isso gera faturamento real para o intent de financeiro do chat.
        # ════════════════════════════════════════════════════════════════════
        random.seed(42)  # reprodutível
        total_revenue = 0.0
        orders_created = 0

        for days_ago in range(14, -1, -1):
            # Mais pedidos em dias de semana, menos no início
            n_orders = random.randint(2, 5) if days_ago < 10 else random.randint(1, 3)
            order_date = datetime.now(timezone.utc) - timedelta(days=days_ago)

            for _ in range(n_orders):
                # Seleciona 1-4 produtos aleatórios por pedido
                n_items = random.randint(1, 4)
                selected_products = random.sample(products, min(n_items, len(products)))

                order_total = 0.0
                order = Order(
                    tenant_id=t_id,
                    total_price=0.0,
                    created_at=order_date + timedelta(
                        hours=random.randint(10, 22),
                        minutes=random.randint(0, 59),
                    ),
                )
                db.add(order)
                await db.flush()

                for prod in selected_products:
                    qty = random.randint(1, 3)
                    item_total = prod.price * qty
                    order_total += item_total

                    order_item = OrderItem(
                        order_id=order.id,
                        product_id=prod.id,
                        quantity=qty,
                        unit_price=prod.price,
                    )
                    db.add(order_item)

                order.total_price = round(order_total, 2)
                total_revenue += order_total
                orders_created += 1

        await db.flush()
        avg_ticket = round(total_revenue / orders_created, 2) if orders_created > 0 else 0
        print(f"💰 {orders_created} pedidos de venda criados")
        print(f"   Faturamento total: R$ {total_revenue:,.2f}")
        print(f"   Ticket médio: R$ {avg_ticket:,.2f}")

        # ════════════════════════════════════════════════════════════════════
        # 8. ESCALAS DE HOJE (4 colaboradores em turnos variados)
        #    Isso garante que o intent "quem trabalha hoje?" retorne dados.
        # ════════════════════════════════════════════════════════════════════
        today = date.today()
        schedules_data = [
            # (user_index, start, end, notes)
            (1, "07:00", "15:00", "Turno da manhã — responsável pelo caixa"),
            (2, "07:00", "13:00", "Supervisão matutina — conferência de estoque"),
            (3, "11:00", "19:00", "Turno intermediário — cozinha e atendimento"),
            (4, "15:00", "23:00", "Turno noturno — fechamento do restaurante"),
        ]

        for user_idx, start, end, notes in schedules_data:
            sched = EmployeeSchedule(
                tenant_id=t_id,
                user_id=users[user_idx].id,
                shift_date=today,
                start_time=start,
                end_time=end,
                notes=notes,
            )
            db.add(sched)

        # Também cria escalas para amanhã (para variedade)
        tomorrow = today + timedelta(days=1)
        for user_idx, start, end, _ in schedules_data[:3]:
            db.add(EmployeeSchedule(
                tenant_id=t_id,
                user_id=users[user_idx].id,
                shift_date=tomorrow,
                start_time=start,
                end_time=end,
                notes="Escala prevista",
            ))

        await db.flush()
        print(f"📅 {len(schedules_data)} escalas para hoje + 3 para amanhã")

        # ════════════════════════════════════════════════════════════════════
        # 9. PEDIDOS DE COMPRA (histórico de reposição de estoque)
        # ════════════════════════════════════════════════════════════════════
        po1 = PurchaseOrder(
            tenant_id=t_id,
            supplier_id=suppliers[0].id,
            status="COMPLETED",
            total_price=1420.00,
            delivery_days=3,
            quality_rating=5,
            price_rating=4,
        )
        db.add(po1)
        await db.flush()
        db.add_all([
            PurchaseItem(purchase_order_id=po1.id, insumo_id=insumos[1].id, quantity=20.0, unit_cost=38.00),
            PurchaseItem(purchase_order_id=po1.id, insumo_id=insumos[11].id, quantity=10.0, unit_cost=48.00),
            PurchaseItem(purchase_order_id=po1.id, insumo_id=insumos[9].id, quantity=5.0, unit_cost=42.00),
        ])

        po2 = PurchaseOrder(
            tenant_id=t_id,
            supplier_id=suppliers[1].id,
            status="COMPLETED",
            total_price=385.00,
            delivery_days=1,
            quality_rating=4,
            price_rating=5,
        )
        db.add(po2)
        await db.flush()
        db.add_all([
            PurchaseItem(purchase_order_id=po2.id, insumo_id=insumos[2].id, quantity=50.0, unit_cost=2.80),
            PurchaseItem(purchase_order_id=po2.id, insumo_id=insumos[3].id, quantity=30.0, unit_cost=7.20),
        ])

        po3 = PurchaseOrder(
            tenant_id=t_id,
            supplier_id=suppliers[2].id,
            status="PENDING",
            total_price=560.00,
        )
        db.add(po3)
        await db.flush()
        db.add_all([
            PurchaseItem(purchase_order_id=po3.id, insumo_id=insumos[5].id, quantity=200.0, unit_cost=2.80),
        ])

        print(f"🚚 3 pedidos de compra criados (2 entregues, 1 pendente)")

        # ════════════════════════════════════════════════════════════════════
        # COMMIT FINAL
        # ════════════════════════════════════════════════════════════════════
        await db.commit()

    # ── Resumo da demonstração ──────────────────────────────────────────
    print("\n" + "=" * 60)
    print("✅ BANCO POPULADO COM SUCESSO PARA DEMONSTRAÇÃO DO CHATBOT!")
    print("=" * 60)
    print()
    print("🔑 LOGIN:")
    print("   Email: joao@saborescia.com")
    print("   Senha: admin123")
    print()
    print("💬 PERGUNTAS PARA TESTAR NO CHATBOT:")
    print('   📊 "Como está meu faturamento?"')
    print('   📊 "Qual é o ticket médio de vendas?"')
    print('   ⚠️  "Quais insumos estão críticos no estoque?"')
    print('   ⚠️  "Tem alguma coisa faltando no estoque?"')
    print('   📅 "Quem está escalado para trabalhar hoje?"')
    print('   📅 "Quem trabalha no turno da noite?"')
    print('   🤝 "Quais fornecedores temos cadastrados?"')
    print('   🤝 "Preciso comprar mais carne, quem é nosso fornecedor?"')
    print('   💬 "Olá, bom dia!" (saudação com sugestões)')
    print()
    print("📊 DADOS GERADOS:")
    print(f"   • {orders_created} pedidos de venda (R$ {total_revenue:,.2f})")
    print(f"   • {len(insumos)} insumos ({n_critical} em ruptura crítica)")
    print(f"   • {len(schedules_data)} escalas para hoje")
    print(f"   • {len(suppliers)} fornecedores")
    print(f"   • {len(products)} produtos com ficha técnica")
    print(f"   • {len(users)} colaboradores")
    print("=" * 60)


if __name__ == "__main__":
    asyncio.run(seed_demo())
