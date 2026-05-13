package br.ufpb.dsc.mercado.controller;

import br.ufpb.dsc.mercado.domain.CategoriaProduto;
import br.ufpb.dsc.mercado.domain.FichaTecnica;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.dto.FichaTecnicaForm;
import br.ufpb.dsc.mercado.service.FichaTecnicaService;
import br.ufpb.dsc.mercado.service.ProdutoService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/produtos/{produtoId}/ficha-tecnica")
public class FichaTecnicaController {

    private final FichaTecnicaService fichaTecnicaService;
    private final ProdutoService produtoService;

    public FichaTecnicaController(FichaTecnicaService fichaTecnicaService, ProdutoService produtoService) {
        this.fichaTecnicaService = fichaTecnicaService;
        this.produtoService = produtoService;
    }

    @GetMapping
    public String abrirFicha(@PathVariable Long produtoId, Model model) {
        Produto produto = produtoService.buscarPorId(produtoId);
        List<FichaTecnica> itens = fichaTecnicaService.listarPorProdutoPai(produtoId);
        
        // Listar apenas insumos para o dropdown de adição
        // Nota: Em um sistema real, usaríamos paginação ou busca dinâmica via HTMX
        List<Produto> insumos = produtoService.listar(org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .filter(p -> p.getCategoria() == CategoriaProduto.INSUMO)
                .collect(Collectors.toList());

        model.addAttribute("produto", produto);
        model.addAttribute("itens", itens);
        model.addAttribute("insumos", insumos);
        model.addAttribute("form", new FichaTecnicaForm(null, null));
        
        return "produtos/fragments/ficha-tecnica :: modal";
    }

    @PostMapping
    public String adicionar(@PathVariable Long produtoId, 
                            @Valid @ModelAttribute("form") FichaTecnicaForm form, 
                            BindingResult bindingResult, 
                            Model model) {
        if (!bindingResult.hasErrors()) {
            fichaTecnicaService.adicionarIngrediente(produtoId, form);
        }
        return abrirFicha(produtoId, model); // Recarrega o fragmento da ficha
    }

    @DeleteMapping("/{itemId}")
    public String remover(@PathVariable Long produtoId, @PathVariable Long itemId, Model model) {
        fichaTecnicaService.removerIngrediente(itemId);
        return abrirFicha(produtoId, model);
    }
}
