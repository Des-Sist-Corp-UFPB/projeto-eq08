package br.ufpb.dsc.mercado.repository;

import br.ufpb.dsc.mercado.domain.FichaTecnica;
import br.ufpb.dsc.mercado.domain.Produto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FichaTecnicaRepository extends JpaRepository<FichaTecnica, Long> {
    List<FichaTecnica> findByProdutoPai(Produto produtoPai);
}
