package com.mecanix.model;

import jakarta.persistence.*;

@Entity
@DiscriminatorValue("REPUESTO")
public class DetalleRepuesto extends DetalleOrden {

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_repuesto", nullable = true)
    private Repuesto repuesto;

    @Override
    public double calcularSubtotal() {
        double precio = getPrecioUnitario() != null ? getPrecioUnitario() : 0.0;
        int cant = getCantidad() != null ? getCantidad() : 1;
        return precio * cant;
    }

    public Repuesto getRepuesto() { return repuesto; }
    public void setRepuesto(Repuesto repuesto) { this.repuesto = repuesto; }
}