package com.mecanix.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mecanix.service.IExportableCsv;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Entity
@Table(name = "MovimientosInventario")
public class MovimientoInventario implements IExportableCsv {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer idMovimiento;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_repuesto", nullable = false)
    private Repuesto repuesto;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_orden", nullable = true)
    private OrdenTrabajo ordenTrabajo;

    @Column(nullable = false, length = 30)
    private String tipoMovimiento;

    @Column(nullable = false)
    private Integer cantidad;

    @Column(nullable = false)
    private Integer stockAnterior;

    @Column(nullable = false)
    private Integer stockNuevo;

    @Column(length = 30)
    private String referenciaOrden;

    @Column(nullable = false, length = 250)
    private String motivo;

    @Column(nullable = false, length = 100)
    private String usuarioResponsable;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    public MovimientoInventario() {}

    public MovimientoInventario(Repuesto repuesto, String tipoMovimiento, Integer cantidad,
                                Integer stockAnterior, Integer stockNuevo, String referenciaOrden,
                                String motivo, String usuarioResponsable) {
        this.repuesto = repuesto;
        this.tipoMovimiento = tipoMovimiento;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockNuevo = stockNuevo;
        this.referenciaOrden = referenciaOrden;
        this.motivo = motivo;
        this.usuarioResponsable = usuarioResponsable;
    }

    @Override
    public String[] obtenerColumnasCsv() {
        return new String[]{"ID", "Codigo", "Repuesto", "Tipo", "Cantidad", "Stock Anterior", "Stock Nuevo", "Orden", "Motivo", "Responsable", "Fecha"};
    }

    @Override
    public String[] obtenerValoresCsv() {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return new String[]{
            String.valueOf(idMovimiento),
            repuesto != null ? repuesto.getCodigo() : "",
            repuesto != null ? repuesto.getNombre() : "",
            tipoMovimiento,
            String.valueOf(cantidad),
            String.valueOf(stockAnterior),
            String.valueOf(stockNuevo),
            referenciaOrden != null ? referenciaOrden : "-",
            motivo,
            usuarioResponsable,
            fechaHora != null ? fechaHora.format(fmt) : ""
        };
    }

    public Integer getIdMovimiento() { return idMovimiento; }
    public Repuesto getRepuesto() { return repuesto; }
    public OrdenTrabajo getOrdenTrabajo() { return ordenTrabajo; }
    public void setOrdenTrabajo(OrdenTrabajo ordenTrabajo) { this.ordenTrabajo = ordenTrabajo; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public Integer getCantidad() { return cantidad; }
    public Integer getStockAnterior() { return stockAnterior; }
    public Integer getStockNuevo() { return stockNuevo; }
    public String getReferenciaOrden() { return referenciaOrden; }
    public String getMotivo() { return motivo; }
    public String getUsuarioResponsable() { return usuarioResponsable; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
}