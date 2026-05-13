package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.FichaTecnica;
import br.ufpb.dsc.mercado.domain.Produto;
import br.ufpb.dsc.mercado.dto.FichaTecnicaForm;
import br.ufpb.dsc.mercado.repository.FichaTecnicaRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FichaTecnicaService {

    private final FichaTecnicaRepository fichaTecnicaRepository;
    private final ProdutoRepository produtoRepository;

    public FichaTecnicaService(FichaTecnicaRepository fichaTecnicaRepository, ProdutoRepository produtoRepository) {
        this.fichaTecnicaRepository = fichaTecnicaRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional(readOnly = true)
    public List<FichaTecnica> listarPorProdutoPai(Long produtoPaiId) {
        Produto produtoPai = produtoRepository.findById(produtoPaiId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
        return fichaTecnicaRepository.findByProdutoPai(produtoPai);
    }

    @Transactional
    public void adicionarIngrediente(Long produtoPaiId, FichaTecnicaForm form) {
        Produto produtoPai = produtoRepository.findById(produtoPaiId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
        Produto insumo = produtoRepository.findById(form.insumoId())
                .orElseThrow(() -> new RuntimeException("Insumo não encontrado"));

        FichaTecnica item = new FichaTecnica(produtoPai, insumo, form.quantidade());
        fichaTecnicaRepository.save(item);
    }

    @Transactional
    public void removerIngrediente(Long id) {
        fichaTecnicaRepository.deleteById(id);
    }
}
