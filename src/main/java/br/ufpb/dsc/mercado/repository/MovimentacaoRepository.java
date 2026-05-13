package br.ufpb.dsc.mercado.repository;

import br.ufpb.dsc.mercado.domain.Movimentacao;
import br.ufpb.dsc.mercado.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MovimentacaoRepository extends JpaRepository<Movimentacao, Long> {
    List<Movimentacao> findByProdutoOrderByDataMovimentacaoDesc(Produto produto);
}
