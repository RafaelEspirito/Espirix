package br.com.espiritolabs.espirix.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    public record ApiError(LocalDateTime timestamp, int status, String erro,
                           String mensagem, String caminho, List<String> detalhes) { }

    private ResponseEntity<ApiError> reply(HttpStatus status, String message,
                                          HttpServletRequest request, List<String> details) {
        return ResponseEntity.status(status).body(new ApiError(LocalDateTime.now(),
            status.value(), status.getReasonPhrase(), message, request.getRequestURI(), details));
    }
    @ExceptionHandler(BusinessException.class)
    ResponseEntity<ApiError> business(BusinessException ex, HttpServletRequest req) {
        return reply(ex.getStatus(), ex.getMessage(), req, List.of());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var details = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage()).toList();
        return reply(HttpStatus.BAD_REQUEST, "Dados de entrada invalidos", req, details);
    }
    @ExceptionHandler({ConstraintViolationException.class, IllegalArgumentException.class})
    ResponseEntity<ApiError> invalid(Exception ex, HttpServletRequest req) {
        return reply(HttpStatus.BAD_REQUEST, ex.getMessage(), req, List.of());
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ResponseEntity<ApiError> maxSize(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return reply(HttpStatus.PAYLOAD_TOO_LARGE, "Arquivo acima do limite configurado (10 MB)", req, List.of());
    }
}
