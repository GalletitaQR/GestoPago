package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.banco.*;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.RecursoNoEncontradoException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 400 - Validaciones de cuerpo de petición (@Valid en DTOs)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> manejarMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("Error de validación en cuerpo de petición: {}", errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse(HttpStatus.BAD_REQUEST.value(), "Error de validación: " + errores));
    }

    // 400 - Validaciones de parámetros (@Positive, @NotNull, etc.)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> manejarConstraintViolation(ConstraintViolationException ex) {
        String mensajeDetalle = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        log.warn("Error de validación en parámetros: {}", mensajeDetalle);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse(HttpStatus.BAD_REQUEST.value(), "Error de validación: " + mensajeDetalle));
    }

    // 400 - Validación de método de Spring
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<GenericResponse> manejarHandlerMethodValidation(HandlerMethodValidationException ex) {
        log.warn("Error de validación de método: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse(HttpStatus.BAD_REQUEST.value(), "Error de validación en parámetros: " + ex.getReason()));
    }

    // 400 - Reglas de Negocio o Contraseñas Inválidas o Parámetros Incorrectos
    @ExceptionHandler({ReglaNegocioException.class, ContrasenaInvalidaException.class, IllegalArgumentException.class})
    public ResponseEntity<GenericResponse> manejarReglaNegocio(RuntimeException ex) {
        log.warn("Regla de negocio no satisfecha: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse(HttpStatus.BAD_REQUEST.value(), ex.getMessage()));
    }

    // 401 - Credenciales Inválidas
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<GenericResponse> manejarCredencialesInvalidas(CredencialesInvalidasException ex) {
        log.warn("Credenciales inválidas: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new GenericResponse(HttpStatus.UNAUTHORIZED.value(), ex.getMessage()));
    }

    // 403 - Usuario Inactivo o Acceso Denegado
    @ExceptionHandler({UsuarioInactivoException.class, AccesoDenegadoException.class, AccessDeniedException.class})
    public ResponseEntity<GenericResponse> manejarAccesoDenegado(RuntimeException ex) {
        log.warn("Acceso denegado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new GenericResponse(HttpStatus.FORBIDDEN.value(), ex.getMessage()));
    }

    // 404 - Recursos No Encontrados
    @ExceptionHandler({ClienteNoEncontradoException.class, UsuarioNoEncontradoException.class,
            CuentaNoEncontradaException.class, RecursoNoEncontradoException.class})
    public ResponseEntity<GenericResponse> manejarRecursoNoEncontrado(RuntimeException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new GenericResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    // 409 - Duplicados y Conflictos
    @ExceptionHandler({ClienteYaRegistradoException.class, CurpDuplicadaException.class,
            RfcDuplicadoException.class, CorreoDuplicadoException.class})
    public ResponseEntity<GenericResponse> manejarDuplicados(RuntimeException ex) {
        log.warn("Conflicto / dato duplicado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new GenericResponse(HttpStatus.CONFLICT.value(), ex.getMessage()));
    }

    // 409 - Violación de Integridad de Datos en Base de Datos (Unique constraints)
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<GenericResponse> manejarDataIntegrityViolation(org.springframework.dao.DataIntegrityViolationException ex) {
        log.warn("Violación de integridad de datos en BD: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new GenericResponse(HttpStatus.CONFLICT.value(),
                        "Conflicto de integridad: Los datos proporcionados violan una restricción única del sistema."));
    }

    // 400 - Error de lectura del cuerpo HTTP (JSON malformado o tipo incompatible)
    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<GenericResponse> manejarHttpMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        log.warn("Cuerpo de petición ilegible o malformado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new GenericResponse(HttpStatus.BAD_REQUEST.value(),
                        "El cuerpo de la petición no tiene un formato JSON válido o contiene tipos de datos incorrectos."));
    }

    // 405 - Método HTTP No Soportado
    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GenericResponse> manejarMetodoNoSoportado(org.springframework.web.HttpRequestMethodNotSupportedException ex) {
        log.warn("Método HTTP no soportado: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new GenericResponse(HttpStatus.METHOD_NOT_ALLOWED.value(),
                        "El método HTTP " + ex.getMethod() + " no está soportado para este endpoint."));
    }

    // 500 - Excepción general no controlada (sin exponer detalles internos sensibles)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> manejarExcepcionGeneral(Exception ex) {
        log.error("Excepción interna no controlada: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new GenericResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                        "Ocurrió un error interno en el servidor al procesar su solicitud."));
    }
}
