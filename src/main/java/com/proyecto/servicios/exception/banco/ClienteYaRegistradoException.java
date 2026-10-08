package com.proyecto.servicios.exception.banco;

public class ClienteYaRegistradoException extends RuntimeException {
    public ClienteYaRegistradoException(String message) {
        super(message);
    }
}
