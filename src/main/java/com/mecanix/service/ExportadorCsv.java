package com.mecanix.service;

import java.util.List;

public class ExportadorCsv<T extends IExportableCsv> {

    public String generarContenido(String[] encabezados, List<T> filas) {
        StringBuilder sb = new StringBuilder();
        sb.append("\uFEFF");
        sb.append(formatearLinea(encabezados)).append("\n");
        if (filas != null) {
            for (T item : filas) {
                sb.append(formatearLinea(item.obtenerValoresCsv())).append("\n");
            }
        }
        return sb.toString();
    }

    private String formatearLinea(String[] campos) {
        if (campos == null || campos.length == 0) return "";
        StringBuilder linea = new StringBuilder();
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) linea.append(",");
            String val = campos[i] != null ? campos[i].replace("\"", "\"\"") : "";
            linea.append("\"").append(val).append("\"");
        }
        return linea.toString();
    }
}