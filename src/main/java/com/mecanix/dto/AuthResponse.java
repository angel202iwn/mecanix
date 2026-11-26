package com.mecanix.dto;

import java.util.List;

public record AuthResponse(
    String token,
    UserInfo user
) {
    public record UserInfo(Integer id, String nombre, String email, String rol, List<String> permisos) {}
}