package br.com.portfolio.financeiro.repository;

import br.com.portfolio.financeiro.domain.ImportBatch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ImportBatchRepository extends JpaRepository<ImportBatch, Long> { }
