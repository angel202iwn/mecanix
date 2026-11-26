package com.mecanix.controller;

import com.mecanix.dto.AuthResponse;
import com.mecanix.dto.LoginRequest;
import com.mecanix.exception.CredencialesInvalidasException;
import com.mecanix.model.Usuario;
import com.mecanix.repository.UsuarioRepository;
import com.mecanix.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos."));

        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new CredencialesInvalidasException("La cuenta está desactivada. Contacta al administrador.");
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPasswordHash())) {
            throw new CredencialesInvalidasException("Correo o contraseña incorrectos.");
        }

        String token = jwtService.generarToken(usuario);
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "CLIENTE";
        var userInfo = new AuthResponse.UserInfo(
                usuario.getIdUsuario(),
                usuario.getNombreCompleto(),
                usuario.getEmail(),
                rolNombre,
                usuario.obtenerPermisos()
        );

        return ResponseEntity.ok(new AuthResponse(token, userInfo));
    }
}