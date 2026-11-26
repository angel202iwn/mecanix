package com.mecanix.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "OrdenesTrabajo")
public class OrdenTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_orden")
    private Integer idOrden;

    @Column(name = "codigo_orden", nullable = false, unique = true, length = 30)
    private String codigoOrden;

    @Column(name = "descripcion_falla", nullable = false, length = 300)
    private String descripcionFalla;

    @Column(name = "informacion_adicional", length = 500)
    private String informacionAdicional;

    @Column(nullable = false, length = 30)
    private String estado = "PENDIENTE";

    @Column(name = "estado_pago", nullable = false, length = 25)
    private String estadoPago = "PENDIENTE_PAGO";

    @Column(name = "metodo_pago", length = 30)
    private String metodoPago;

    @Column(name = "fecha_pago")
    private LocalDateTime fechaPago;

    @Column(name = "costo_estimado", nullable = false)
    private Double costoEstimado = 0.0;

    @Column(name = "fecha_ingreso", nullable = false)
    private LocalDateTime fechaIngreso = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_vehiculo", nullable = false)
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_mecanico", nullable = true)
    private Usuario mecanicoAsignado;

    public Integer getIdOrden() { return idOrden; }
    public void setIdOrden(Integer idOrden) { this.idOrden = idOrden; }
    public String getCodigoOrden() { return codigoOrden; }
    public void setCodigoOrden(String codigoOrden) { this.codigoOrden = codigoOrden; }
    public String getDescripcionFalla() { return descripcionFalla; }
    public void setDescripcionFalla(String descripcionFalla) { this.descripcionFalla = descripcionFalla; }
    public String getInformacionAdicional() { return informacionAdicional; }
    public void setInformacionAdicional(String informacionAdicional) { this.informacionAdicional = informacionAdicional; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public LocalDateTime getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDateTime fechaPago) { this.fechaPago = fechaPago; }
    public Double getCostoEstimado() { return costoEstimado; }
    public void setCostoEstimado(Double costoEstimado) { this.costoEstimado = costoEstimado; }
    public LocalDateTime getFechaIngreso() { return fechaIngreso; }
    public void setFechaIngreso(LocalDateTime fechaIngreso) { this.fechaIngreso = fechaIngreso; }
    public Vehiculo getVehiculo() { return vehiculo; }
    public void setVehiculo(Vehiculo vehiculo) { this.vehiculo = vehiculo; }
    public Usuario getMecanicoAsignado() { return mecanicoAsignado; }
    public void setMecanicoAsignado(Usuario mecanicoAsignado) { this.mecanicoAsignado = mecanicoAsignado; }
}