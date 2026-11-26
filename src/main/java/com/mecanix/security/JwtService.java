package com.mecanix.security;

import com.mecanix.model.Usuario;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationMs;

    public String generarToken(Usuario usuario) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        String rolNombre = usuario.getRol() != null ? usuario.getRol().getNombre() : "CLIENTE";
        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("rol", rolNombre)
                .claim("nombre", usuario.getNombreCompleto())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }
}