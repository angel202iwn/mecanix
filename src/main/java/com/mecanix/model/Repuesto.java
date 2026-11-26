package com.mecanix.model;

import com.mecanix.exception.StockInsuficienteException;
import jakarta.persistence.*;

@Entity
@Table(name = "Repuestos")
public class Repuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_repuesto")
    private Integer idRepuesto;

    @Column(nullable = false, unique = true, length = 30)
    private String codigo;

    @Column(nullable = false, length = 150)
    private String nombre;

    @Column(name = "precio_unitario", nullable = false)
    private Double precioUnitario;

    @Column(nullable = false)
    private Integer stock;

    public void descontarStock(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a descontar debe ser mayor a cero.");
        }
        int actual = this.stock != null ? this.stock : 0;
        if (cantidad > actual) {
            throw new StockInsuficienteException("Stock insuficiente para " + this.nombre + " (disponible: " + actual + ").");
        }
        this.stock = actual - cantidad;
    }

    public void reponerStock(int cantidad) {
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad a reponer debe ser mayor a cero.");
        }
        int actual = this.stock != null ? this.stock : 0;
        this.stock = actual + cantidad;
    }

    public void ajustarStockManual(int nuevoStock) {
        if (nuevoStock < 0) {
            throw new StockInsuficienteException("El stock ajustado no puede ser negativo.");
        }
        this.stock = nuevoStock;
    }

    public Integer getIdRepuesto() { return idRepuesto; }
    public void setIdRepuesto(Integer idRepuesto) { this.idRepuesto = idRepuesto; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Double getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(Double precioUnitario) { this.precioUnitario = precioUnitario; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}