package com.proyecto.servicios.config;

import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.RecursoNoEncontradoException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<GenericResponse> manejarRecursoNoEncontrado(RecursoNoEncontradoException ex) {
        log.warn("Recurso no encontrado: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.NOT_FOUND.value());
        response.setMensaje(ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<GenericResponse> manejarConstraintViolation(ConstraintViolationException ex) {
        String mensajeDetalle = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("; "));

        log.warn("Error de validación en parámetros de entrada: {}", mensajeDetalle);

        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.BAD_REQUEST.value());
        response.setMensaje("Error de validación en parámetros de entrada: " + mensajeDetalle);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> manejarMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Error de validación en cuerpo de petición: {}", errores);

        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.BAD_REQUEST.value());
        response.setMensaje("Error de validación en los datos enviados: " + errores);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<GenericResponse> manejarHandlerMethodValidation(HandlerMethodValidationException ex) {
        log.warn("Error de validación de método: {}", ex.getMessage());
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.BAD_REQUEST.value());
        response.setMensaje("Error de validación en parámetros: " + ex.getReason());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericResponse> manejarExcepcionGeneral(Exception ex) {
        log.error("Excepción interna no controlada: {}", ex.getMessage(), ex);
        GenericResponse response = new GenericResponse();
        response.setCodigo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.setMensaje("Error interno del servidor: " + ex.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
