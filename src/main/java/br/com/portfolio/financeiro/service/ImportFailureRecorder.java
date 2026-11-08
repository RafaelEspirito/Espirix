package br.com.portfolio.financeiro.service;

import br.com.portfolio.financeiro.domain.*;
import br.com.portfolio.financeiro.repository.ImportBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ImportFailureRecorder {
    private final ImportBatchRepository batches;

    /** A falha deve sobreviver mesmo quando todos os titulos da importacao sao revertidos. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void gravar(String name, OrigemArquivo type, String checksum, String reason) {
        ImportBatch batch = new ImportBatch();
        batch.setNomeArquivo(name);
        batch.setTipoArquivo(type);
        batch.setSha256(checksum);
        batch.setStatus(StatusImportacao.FALHA);
        batch.setMensagem(reason == null ? "Falha de processamento" : reason.substring(0, Math.min(590, reason.length())));
        batches.save(batch);
    }
}
