package com.mecanix.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("SERVICIO")
public class DetalleServicio extends DetalleOrden {

    @Column(name = "horas_tecnicas")
    private Double horasTecnicas = 1.0;

    @Override
    public double calcularSubtotal() {
        double precio = getPrecioUnitario() != null ? getPrecioUnitario() : 0.0;
        int cant = getCantidad() != null ? getCantidad() : 1;
        return precio * cant;
    }

    public Double getHorasTecnicas() { return horasTecnicas; }
    public void setHorasTecnicas(Double horasTecnicas) { this.horasTecnicas = horasTecnicas; }
}