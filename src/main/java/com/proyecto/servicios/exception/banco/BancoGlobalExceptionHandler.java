package com.proyecto.servicios.exception.banco;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class BancoGlobalExceptionHandler {

    @ExceptionHandler(ClienteYaRegistradoException.class)
    public ResponseEntity<Map<String, Object>> handleClienteYaRegistrado(ClienteYaRegistradoException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Cliente Ya Registrado", ex.getMessage());
    }

    @ExceptionHandler(CurpDuplicadaException.class)
    public ResponseEntity<Map<String, Object>> handleCurpDuplicada(CurpDuplicadaException ex) {
        return buildResponse(HttpStatus.CONFLICT, "CURP Duplicada", ex.getMessage());
    }

    @ExceptionHandler(RfcDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleRfcDuplicado(RfcDuplicadoException ex) {
        return buildResponse(HttpStatus.CONFLICT, "RFC Duplicado", ex.getMessage());
    }

    @ExceptionHandler(CorreoDuplicadoException.class)
    public ResponseEntity<Map<String, Object>> handleCorreoDuplicado(CorreoDuplicadoException ex) {
        return buildResponse(HttpStatus.CONFLICT, "Correo Electrónico Duplicado", ex.getMessage());
    }

    @ExceptionHandler(ClienteNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleClienteNoEncontrado(ClienteNoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Cliente No Encontrado", ex.getMessage());
    }

    @ExceptionHandler(CuentaNoEncontradaException.class)
    public ResponseEntity<Map<String, Object>> handleCuentaNoEncontrada(CuentaNoEncontradaException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Cuenta No Encontrada", ex.getMessage());
    }

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioNoEncontrado(UsuarioNoEncontradoException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, "Usuario No Encontrado", ex.getMessage());
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<Map<String, Object>> handleUsuarioInactivo(UsuarioInactivoException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "Usuario Inactivo", ex.getMessage());
    }

    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, Object>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Credenciales Inválidas", ex.getMessage());
    }

    @ExceptionHandler(ContrasenaInvalidaException.class)
    public ResponseEntity<Map<String, Object>> handleContrasenaInvalida(ContrasenaInvalidaException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Contraseña Inválida", ex.getMessage());
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> handleReglaNegocio(ReglaNegocioException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "Violación de Regla de Negocio", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("codigo", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Error de Validación");

        Map<String, String> erroresCampos = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            erroresCampos.put(fieldName, errorMessage);
        });
        body.put("detalles", erroresCampos);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneralException(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error Interno del Servidor", ex.getMessage());
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String error, String mensaje) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("codigo", status.value());
        body.put("error", error);
        body.put("mensaje", mensaje);
        return new ResponseEntity<>(body, status);
    }
}
