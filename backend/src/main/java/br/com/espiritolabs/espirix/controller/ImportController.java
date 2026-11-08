package br.com.espiritolabs.espirix.controller;

import br.com.espiritolabs.espirix.domain.OrigemArquivo;
import br.com.espiritolabs.espirix.domain.TipoOperacao;
import br.com.espiritolabs.espirix.dto.ApiDtos.ImportacaoResponse;
import br.com.espiritolabs.espirix.exception.BusinessException;
import br.com.espiritolabs.espirix.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.security.*;
import java.util.Locale;

@RestController @RequestMapping("/api/importacoes") @RequiredArgsConstructor
public class ImportController {
    private final ImportService service;
    private final ImportFailureRecorder failures;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportacaoResponse> importar(
            @RequestPart("arquivo") MultipartFile arquivo,
            @RequestParam(required = false) TipoOperacao tipoOperacao) {
        String name = arquivo.getOriginalFilename() == null ? "sem-nome" : arquivo.getOriginalFilename();
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        if (name.length() > 255) name = name.substring(name.length() - 255);
        String ext = name.toLowerCase(Locale.ROOT);
        OrigemArquivo type;
        if (ext.endsWith(".csv")) type = OrigemArquivo.CSV;
        else if (ext.endsWith(".pdf")) type = OrigemArquivo.PDF;
        else throw BusinessException.badRequest("Formato nao suportado: envie CSV ou PDF");
        if (arquivo.isEmpty()) throw BusinessException.badRequest("Arquivo vazio");
        try {
            byte[] bytes = arquivo.getBytes();
            String checksum = sha256(bytes);
            try {
                return ResponseEntity.status(HttpStatus.CREATED).body(service.importar(name, type, bytes, checksum, tipoOperacao));
            } catch (RuntimeException | IOException e) {
                if (e instanceof BusinessException business) {
                    failures.gravar(name, type, checksum, business.getMessage());
                    throw business;
                }
                if (e instanceof IllegalArgumentException || e instanceof IOException) {
                    failures.gravar(name, type, checksum, e.getMessage());
                    throw BusinessException.badRequest("Falha na importacao: " + e.getMessage());
                }
                failures.gravar(name, type, checksum, "Falha de persistencia ou processamento inesperado");
                throw e;
            }
        } catch (IOException e) {
            throw BusinessException.badRequest("Falha ao ler arquivo enviado");
        }
    }
    @GetMapping
    public Page<ImportacaoResponse> listar(@RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "20") int size) {
        if (size < 1 || size > 100 || page < 0) throw BusinessException.badRequest("Paginacao invalida");
        return service.historico(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "dataImportacao")));
    }
    private static String sha256(byte[] bytes) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(bytes);
            return java.util.HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
}
