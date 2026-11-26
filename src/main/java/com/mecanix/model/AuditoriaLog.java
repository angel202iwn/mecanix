package com.mecanix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mecanix.service.IExportableCsv;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "AuditoriaLogs")
public class AuditoriaLog implements IExportableCsv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idLog;

    @Column(nullable = false, length = 50)
    private String modulo;

    @Column(nullable = false, length = 250)
    private String accion;

    @Column(nullable = false, length = 100)
    private String usuarioResponsable;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = true)
    private Usuario usuario;

    public AuditoriaLog() {}

    public AuditoriaLog(String modulo, String accion, String usuarioResponsable) {
        this.modulo = modulo;
        this.accion = accion;
        this.usuarioResponsable = usuarioResponsable;
    }

    @Override
    public String[] obtenerColumnasCsv() {
        return new String[]{"ID", "Modulo", "Accion Registrada", "Responsable", "Fecha y Hora"};
    }

    @Override
    public String[] obtenerValoresCsv() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return new String[]{
            String.valueOf(this.idLog),
            this.modulo,
            this.accion,
            this.usuarioResponsable,
            this.fechaHora != null ? this.fechaHora.format(fmt) : ""
        };
    }

    public Integer getIdLog() { return idLog; }
    public String getModulo() { return modulo; }
    public String getAccion() { return accion; }
    public String getUsuarioResponsable() { return usuarioResponsable; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
}