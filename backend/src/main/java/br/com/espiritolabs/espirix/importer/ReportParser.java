package br.com.espiritolabs.espirix.importer;

import java.io.IOException;
import java.util.List;

public interface ReportParser {
    List<ImportedInvoice> parse(byte[] data) throws IOException;
}
