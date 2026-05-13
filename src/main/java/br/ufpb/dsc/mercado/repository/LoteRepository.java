package br.ufpb.dsc.mercado.repository;

import br.ufpb.dsc.mercado.domain.Lote;
import br.ufpb.dsc.mercado.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface LoteRepository extends JpaRepository<Lote, Long> {

    List<Lote> findByProdutoOrderByDataValidadeAsc(Produto produto);

    @Query("SELECT SUM(l.quantidadeAtual) FROM Lote l WHERE l.produto = :produto")
    BigDecimal sumQuantidadeByProduto(@Param("produto") Produto produto);

    @Query("SELECT l FROM Lote l WHERE l.produto = :produto AND l.quantidadeAtual > 0 ORDER BY l.dataValidade ASC NULLS LAST")
    List<Lote> findDisponiveisPorProduto(@Param("produto") Produto produto);
}
