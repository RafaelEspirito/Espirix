package br.com.espiritolabs.espirix.service;

import br.com.espiritolabs.espirix.domain.*;
import br.com.espiritolabs.espirix.dto.ApiDtos.*;
import java.time.LocalDate;

public final class DtoMapper {
    private DtoMapper() { }
    public static ClienteResponse cliente(Client c) {
        return new ClienteResponse(c.getId(), c.getRazaoSocial(), c.getNomeFantasia(),
                c.getCnpjCpf(), c.getDataCadastro());
    }
    public static TituloResponse titulo(Invoice i, LocalDate hoje) {
        return new TituloResponse(i.getId(), i.getNumeroNota(), i.getNumeroParcela(),
                cliente(i.getCliente()), i.getTipoOperacao(), i.getDataEmissao(), i.getDataVencimento(),
                i.getDataPagamento(), i.getValorOriginal(), i.getValorPago(), i.getSaldoDevedor(),
                i.statusEm(hoje), i.emAtraso(hoje), i.getOrigemArquivo());
    }
    public static PagamentoResponse pagamento(Payment p) {
        return new PagamentoResponse(p.getId(), p.getTitulo().getId(), p.getValor(), p.getDataPagamento(),
                p.getOrigem(), p.getObservacao(), p.getRegistradoEm());
    }
    public static ImportacaoResponse importacao(ImportBatch b) {
        return new ImportacaoResponse(b.getId(), b.getNomeArquivo(), b.getTipoArquivo(),
                b.getDataImportacao(), b.getQuantidadeRegistrosNovos(), b.getQuantidadeAtualizados(),
                b.getQuantidadeIgnorados(), b.getStatus(), b.getMensagem());
    }
}
