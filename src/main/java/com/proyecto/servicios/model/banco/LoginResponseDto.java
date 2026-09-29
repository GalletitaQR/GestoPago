package com.proyecto.servicios.model.banco;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponseDto {

    private String token;
    @Builder.Default
    private String tipoToken = "Bearer";
    private String correo;
    private Long usuarioId;
    private Long clienteId;
    private long expiracionMs;
}
