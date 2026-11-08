package br.com.portfolio.financeiro.importer;

import java.io.IOException;
import java.util.List;

public interface ReportParser {
    List<ImportedInvoice> parse(byte[] data) throws IOException;
}
