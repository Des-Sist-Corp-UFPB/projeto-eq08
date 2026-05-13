package br.ufpb.dsc.mercado.service;

import br.ufpb.dsc.mercado.domain.*;
import br.ufpb.dsc.mercado.repository.LoteRepository;
import br.ufpb.dsc.mercado.repository.MovimentacaoRepository;
import br.ufpb.dsc.mercado.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
public class EstoqueService {

    private final LoteRepository loteRepository;
    private final MovimentacaoRepository movimentacaoRepository;
    private final ProdutoRepository produtoRepository;

    public EstoqueService(LoteRepository loteRepository, 
                          MovimentacaoRepository movimentacaoRepository, 
                          ProdutoRepository produtoRepository) {
        this.loteRepository = loteRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional
    public void registrarEntrada(Long produtoId, BigDecimal quantidade, LocalDate dataValidade, String motivo) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        Lote lote = new Lote(produto, quantidade, dataValidade);
        loteRepository.save(lote);

        Movimentacao mov = new Movimentacao(produto, TipoMovimentacao.ENTRADA, quantidade, motivo);
        mov.setLote(lote);
        movimentacaoRepository.save(mov);
    }

    @Transactional
    public void registrarSaida(Long produtoId, BigDecimal quantidade, String motivo) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));

        List<Lote> lotesDisponiveis = loteRepository.findDisponiveisPorProduto(produto);
        BigDecimal restante = quantidade;

        for (Lote lote : lotesDisponiveis) {
            if (restante.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal qtdLote = lote.getQuantidadeAtual();
            if (qtdLote.compareTo(restante) >= 0) {
                lote.setQuantidadeAtual(qtdLote.subtract(restante));
                restante = BigDecimal.ZERO;
            } else {
                lote.setQuantidadeAtual(BigDecimal.ZERO);
                restante = restante.subtract(qtdLote);
            }
            loteRepository.save(lote);
        }

        if (restante.compareTo(BigDecimal.ZERO) > 0) {
            throw new RuntimeException("Estoque insuficiente para o produto: " + produto.getNome());
        }

        Movimentacao mov = new Movimentacao(produto, TipoMovimentacao.SAIDA, quantidade, motivo);
        movimentacaoRepository.save(mov);
    }

    @Transactional(readOnly = true)
    public BigDecimal getSaldoEstoque(Long produtoId) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
        BigDecimal saldo = loteRepository.sumQuantidadeByProduto(produto);
        return saldo != null ? saldo : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public boolean isEstoqueBaixo(Long produtoId) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new RuntimeException("Produto não encontrado"));
        BigDecimal saldo = getSaldoEstoque(produtoId);
        return saldo.compareTo(produto.getEstoqueMinimo()) < 0;
    }
}
