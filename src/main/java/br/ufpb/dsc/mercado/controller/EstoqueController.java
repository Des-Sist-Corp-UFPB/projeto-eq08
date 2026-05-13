package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.domain.TipoMovimentacao;
import br.ufpb.dsc.mercado.dto.MovimentacaoForm;
import br.ufpb.dsc.mercado.service.EstoqueService;
import br.ufpb.dsc.mercado.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/estoque")
public class EstoqueController {

    private final EstoqueService estoqueService;
    private final ProdutoService produtoService;

    public EstoqueController(EstoqueService estoqueService, ProdutoService produtoService) {
        this.estoqueService = estoqueService;
        this.produtoService = produtoService;
    }

    @GetMapping("/movimentar/{produtoId}")
    public String formMovimentacao(@PathVariable Long produtoId, Model model) {
        Produto produto = produtoService.buscarPorId(produtoId);
        model.addAttribute("produto", produto);
        model.addAttribute("form", new MovimentacaoForm(produtoId, TipoMovimentacao.ENTRADA, null, null, null));
        model.addAttribute("tipos", TipoMovimentacao.values());
        return "estoque/fragments/form-movimentacao :: modal";
    }

    @PostMapping("/movimentar")
    public String movimentar(@Valid @ModelAttribute("form") MovimentacaoForm form, 
                             BindingResult bindingResult, 
                             Model model) {
        if (bindingResult.hasErrors()) {
            Produto produto = produtoService.buscarPorId(form.produtoId());
            model.addAttribute("produto", produto);
            model.addAttribute("tipos", TipoMovimentacao.values());
            return "estoque/fragments/form-movimentacao :: modal";
        }

        if (form.tipo() == TipoMovimentacao.ENTRADA) {
            estoqueService.registrarEntrada(form.produtoId(), form.quantidade(), form.dataValidade(), form.motivo());
        } else {
            estoqueService.registrarSaida(form.produtoId(), form.quantidade(), form.motivo());
        }

        // Retorna um fragmento de sucesso ou recarrega a linha do produto
        Produto produto = produtoService.buscarPorId(form.produtoId());
        model.addAttribute("produto", produto);
        model.addAttribute("estoqueService", estoqueService);
        return "produtos/fragments/linha :: linha";
    }
}
