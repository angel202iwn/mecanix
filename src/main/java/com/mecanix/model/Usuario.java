package com.mecanix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "Usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "nombre_completo", nullable = false, length = 120)
    private String nombreCompleto;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @JsonIgnore
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false)
    private Boolean activo = true;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_rol", nullable = false)
    private Rol rol;

    public List<String> obtenerPermisos() {
        if (this.rol == null || this.rol.getNombre() == null) {
            return List.of();
        }
        return switch (this.rol.getNombre().toUpperCase()) {
            case "ADMIN" -> List.of("GESTION_TOTAL", "REPORTES_FINANCIEROS", "USUARIOS", "ORDENES", "INVENTARIO", "CITAS");
            case "RECEPCIONISTA" -> List.of("CITAS", "ORDENES", "CLIENTES", "COTIZACIONES", "DOCUMENTOS");
            case "MECANICO" -> List.of("MIS_ORDENES", "DIAGNOSTICO", "SOLICITAR_REPUESTOS", "KARDEX");
            case "CLIENTE" -> List.of("VER_MI_VEHICULO", "MIS_CITAS", "MIS_BOLETAS");
            default -> List.of();
        };
    }

    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer idUsuario) { this.idUsuario = idUsuario; }
    public String getNombreCompleto() { return nombreCompleto; }
    public void setNombreCompleto(String nombreCompleto) { this.nombreCompleto = nombreCompleto; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }
}