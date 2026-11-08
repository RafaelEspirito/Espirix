package br.com.espiritolabs.espirix.service;

import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.repository.ImportBatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class ImportFailureRecorder {
    private final ImportBatchRepository batches;
    private final br.com.espiritolabs.espirix.repository.CompanyRepository companies;
    private final br.com.espiritolabs.espirix.security.EmpresaAccess empresa;

    /** A falha deve sobreviver mesmo quando todos os titulos da importacao sao revertidos. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void gravar(String name, OrigemArquivo type, String checksum, String reason) {
        companies.findLockedById(empresa.id()).orElseThrow();
        ImportBatch batch = new ImportBatch();
        batch.setEmpresaId(empresa.id());
        batch.setNomeArquivo(name);
        batch.setTipoArquivo(type);
        batch.setSha256(checksum);
        batch.setStatus(StatusImportacao.FALHA);
        batch.setMensagem(reason == null ? "Falha de processamento" : reason.substring(0, Math.min(590, reason.length())));
        batches.save(batch);
    }
}
