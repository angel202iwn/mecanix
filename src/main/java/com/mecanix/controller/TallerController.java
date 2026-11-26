package com.mecanix.controller;

import com.mecanix.model.*;
import com.mecanix.repository.*;
import com.mecanix.service.ExportadorCsv;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/taller")
public class TallerController {

    private final OrdenTrabajoRepository ordenRepo;
    private final ClienteRepository clienteRepo;
    private final VehiculoRepository vehiculoRepo;
    private final RepuestoRepository repuestoRepo;
    private final UsuarioRepository usuarioRepo;
    private final RolRepository rolRepo;
    private final DetalleOrdenRepository detalleRepo;
    private final DocumentoEmitidoRepository docRepo;
    private final AuditoriaLogRepository auditRepo;
    private final MovimientoInventarioRepository movRepo;
    private final CitaTallerRepository citaRepo;
    private final PasswordEncoder passwordEncoder;

    public TallerController(OrdenTrabajoRepository ordenRepo, ClienteRepository clienteRepo,
                            VehiculoRepository vehiculoRepo, RepuestoRepository repuestoRepo,
                            UsuarioRepository usuarioRepo, RolRepository rolRepo,
                            DetalleOrdenRepository detalleRepo, DocumentoEmitidoRepository docRepo,
                            AuditoriaLogRepository auditRepo, MovimientoInventarioRepository movRepo,
                            CitaTallerRepository citaRepo, PasswordEncoder passwordEncoder) {
        this.ordenRepo = ordenRepo;
        this.clienteRepo = clienteRepo;
        this.vehiculoRepo = vehiculoRepo;
        this.repuestoRepo = repuestoRepo;
        this.usuarioRepo = usuarioRepo;
        this.rolRepo = rolRepo;
        this.detalleRepo = detalleRepo;
        this.docRepo = docRepo;
        this.auditRepo = auditRepo;
        this.movRepo = movRepo;
        this.citaRepo = citaRepo;
        this.passwordEncoder = passwordEncoder;
    }

    private AuditoriaLog registrarAuditoria(String modulo, String accion, String responsable) {
        AuditoriaLog log = new AuditoriaLog(modulo, accion, responsable);
        if (responsable != null && !responsable.isBlank()) {
            usuarioRepo.findAll().stream()
                    .filter(u -> responsable.equalsIgnoreCase(u.getNombreCompleto())
                            || responsable.equalsIgnoreCase(u.getEmail()))
                    .findFirst()
                    .ifPresent(log::setUsuario);
        }
        return auditRepo.save(log);
    }

    private String generarCodigoOrdenUnico() {
        List<OrdenTrabajo> existentes = ordenRepo.findAll();
        int maxNum = 0;
        for (OrdenTrabajo o : existentes) {
            String cod = o.getCodigoOrden();
            if (cod != null && cod.startsWith("OT-2026-")) {
                try {
                    int num = Integer.parseInt(cod.substring("OT-2026-".length()).trim());
                    if (num > maxNum) {
                        maxNum = num;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        int siguiente = maxNum + 1;
        while (true) {
            String candidato = "OT-2026-" + String.format("%04d", siguiente);
            boolean existe = false;
            for (OrdenTrabajo o : existentes) {
                if (candidato.equalsIgnoreCase(o.getCodigoOrden())) {
                    existe = true;
                    break;
                }
            }
            if (!existe) {
                return candidato;
            }
            siguiente++;
        }
    }

    // 1. RESUMEN KPI
    @GetMapping("/resumen")
    public Map<String, Object> obtenerResumen() {
        List<OrdenTrabajo> todas = ordenRepo.findAll();
        long activas = todas.stream().filter(o -> !"LISTO".equals(o.getEstado())).count();
        long cotizaciones = todas.stream().filter(o -> "PENDIENTE".equals(o.getEstado())).count();
        double facturado = todas.stream().filter(o -> "LISTO".equals(o.getEstado()))
                .mapToDouble(o -> o.getCostoEstimado() != null ? o.getCostoEstimado() : 0.0).sum();
        double porCobrar = todas.stream().filter(o -> !"LISTO".equals(o.getEstado()))
                .mapToDouble(o -> o.getCostoEstimado() != null ? o.getCostoEstimado() : 0.0).sum();

        Map<String, Object> kpis = new HashMap<>();
        kpis.put("totalClientes", clienteRepo.count());
        kpis.put("totalVehiculos", vehiculoRepo.count());
        kpis.put("ordenesActivas", activas);
        kpis.put("totalOrdenes", todas.size());
        kpis.put("cotizacionesPendientes", cotizaciones);
        kpis.put("totalProductos", repuestoRepo.count());
        kpis.put("repuestosBajoStock", repuestoRepo.findAll().stream().filter(r -> r.getStock() <= 5).count());
        kpis.put("ingresosFacturados", facturado);
        kpis.put("porCobrar", porCobrar);
        kpis.put("documentosEmitidos", docRepo.count());
        return kpis;
    }

    // 2. CITAS Y AGENDA DEL TALLER
    @GetMapping("/citas")
    public List<CitaTaller> listarCitas(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nombre) {
        List<CitaTaller> todas = citaRepo.findAllByOrderByFechaHoraCitaDesc();
        if ("CLIENTE".equalsIgnoreCase(rol)) {
            return todas.stream().filter(c -> {
                boolean matchEmail = email != null && !email.isBlank()
                        && c.getClienteEmail() != null && c.getClienteEmail().equalsIgnoreCase(email.trim());
                boolean matchNombre = nombre != null && !nombre.isBlank()
                        && c.getClienteNombre() != null && c.getClienteNombre().toLowerCase().contains(nombre.trim().toLowerCase());
                return matchEmail || matchNombre;
            }).toList();
        }
        return todas;
    }

    @PostMapping("/citas")
    @Transactional
    public ResponseEntity<CitaTaller> crearCita(@RequestBody Map<String, Object> body) {
        String placa = body.getOrDefault("placa", "").toString().trim().toUpperCase();
        String dni = body.getOrDefault("dni", "").toString().trim();
        String clienteInput = body.getOrDefault("cliente", "").toString().trim();
        String emailInput = body.getOrDefault("email", "").toString().trim();
        String telefonoInput = body.getOrDefault("telefono", "").toString().trim();
        String motivo = body.getOrDefault("motivo", "Diagnóstico computarizado / Scanner").toString().trim();
        String responsable = body.getOrDefault("responsable", "Recepción").toString().trim();

        LocalDateTime fechaHora;
        if (body.get("fechaHora") != null && !body.get("fechaHora").toString().isBlank()) {
            fechaHora = LocalDateTime.parse(body.get("fechaHora").toString().replace(" ", "T"));
        } else {
            fechaHora = LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
        }

        Vehiculo vehiculoEncontrado = !placa.isBlank()
                ? vehiculoRepo.findByPlacaIgnoreCase(placa).orElse(null)
                : null;

        Cliente clienteEncontrado = null;
        if (!dni.isBlank()) {
            clienteEncontrado = clienteRepo.findByDniRuc(dni).orElse(null);
        }
        if (clienteEncontrado == null && !emailInput.isBlank()) {
            clienteEncontrado = clienteRepo.findAll().stream()
                    .filter(c -> c.getEmail() != null && emailInput.equalsIgnoreCase(c.getEmail().trim()))
                    .findFirst().orElse(null);
        }
        if (clienteEncontrado == null && !clienteInput.isBlank()) {
            clienteEncontrado = clienteRepo.findAll().stream()
                    .filter(c -> c.getNombreCompleto() != null && c.getNombreCompleto().equalsIgnoreCase(clienteInput))
                    .findFirst().orElse(null);
        }
        if (clienteEncontrado == null && vehiculoEncontrado != null) {
            clienteEncontrado = vehiculoEncontrado.getCliente();
        }

        CitaTaller cita = new CitaTaller();
        cita.setFechaHoraCita(fechaHora);
        cita.setPlacaVehiculo(vehiculoEncontrado != null ? vehiculoEncontrado.getPlaca() : (placa.isBlank() ? "POR-DEFINIR" : placa));
        cita.setClienteNombre(clienteEncontrado != null
                ? clienteEncontrado.getNombreCompleto()
                : (clienteInput.isBlank() ? "Cliente Mostrador" : clienteInput));
        cita.setClienteEmail(clienteEncontrado != null && clienteEncontrado.getEmail() != null
                ? clienteEncontrado.getEmail() : emailInput);
        cita.setClienteTelefono(clienteEncontrado != null && clienteEncontrado.getTelefono() != null
                ? clienteEncontrado.getTelefono() : telefonoInput);
        cita.setMotivo(motivo);
        cita.setEstado("PENDIENTE");
        cita.setRegistradoPor(responsable);
        cita.setCliente(clienteEncontrado);
        cita.setVehiculo(vehiculoEncontrado);

        CitaTaller guardada = citaRepo.save(cita);
        registrarAuditoria("ORDENES", "Agendó cita #" + guardada.getIdCita() + " (" + guardada.getPlacaVehiculo() + " · " + guardada.getMotivo() + ")", responsable);
        return ResponseEntity.ok(guardada);
    }

    @PatchMapping("/citas/{id}/estado")
    @Transactional
    public ResponseEntity<?> actualizarEstadoCita(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        CitaTaller cita = citaRepo.findById(id).orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        String nuevoEstado = body.getOrDefault("estado", "CONFIRMADA").toString().toUpperCase();
        String responsable = body.getOrDefault("responsable", "Recepción / Admin").toString();
        cita.setEstado(nuevoEstado);
        CitaTaller actualizada = citaRepo.saveAndFlush(cita);

        Map<String, Object> ordenInfo = null;
        if ("ATENDIDA".equals(nuevoEstado)) {
            Cliente cliente = cita.getCliente();
            if (cliente == null) {
                String dniBase = "CITA-" + cita.getIdCita();
                cliente = clienteRepo.findByDniRuc(dniBase).orElseGet(() -> {
                    Cliente nuevo = new Cliente();
                    nuevo.setDniRuc(dniBase);
                    nuevo.setNombreCompleto(cita.getClienteNombre() != null ? cita.getClienteNombre() : "Cliente Cita");
                    nuevo.setTelefono(cita.getClienteTelefono() != null && !cita.getClienteTelefono().isBlank() ? cita.getClienteTelefono() : "999999999");
                    nuevo.setEmail(cita.getClienteEmail() != null && !cita.getClienteEmail().isBlank() ? cita.getClienteEmail() : "cliente@mecanix.com");
                    return clienteRepo.saveAndFlush(nuevo);
                });
            }

            Vehiculo vehiculo = cita.getVehiculo();
            if (vehiculo == null) {
                String placaCita = (cita.getPlacaVehiculo() != null && !cita.getPlacaVehiculo().isBlank())
                        ? cita.getPlacaVehiculo().trim().toUpperCase()
                        : ("VEH-" + cita.getIdCita());
                final Cliente cliFinal = cliente;
                vehiculo = vehiculoRepo.findByPlacaIgnoreCase(placaCita).orElseGet(() -> {
                    Vehiculo v = new Vehiculo();
                    v.setPlaca(placaCita);
                    v.setMarca("Por verificar");
                    v.setModelo("Unidad de cita");
                    v.setAnio(2024);
                    v.setCliente(cliFinal);
                    return vehiculoRepo.saveAndFlush(v);
                });
            }

            double costoBase = body.get("costo") != null && !body.get("costo").toString().isBlank()
                    ? Double.parseDouble(body.get("costo").toString())
                    : 150.0;

            OrdenTrabajo orden = new OrdenTrabajo();
            orden.setCodigoOrden(generarCodigoOrdenUnico());
            orden.setDescripcionFalla(cita.getMotivo());
            orden.setInformacionAdicional("");
            orden.setCostoEstimado(costoBase);
            orden.setEstado("PENDIENTE");
            orden.setVehiculo(vehiculo);

            Object rawIdMec = body.get("idMecanico");
            if (rawIdMec != null && !rawIdMec.toString().isBlank() && !"SIN_ASIGNAR".equalsIgnoreCase(rawIdMec.toString())) {
                usuarioRepo.findById(Integer.valueOf(rawIdMec.toString())).ifPresent(orden::setMecanicoAsignado);
            } else {
                orden.setMecanicoAsignado(null);
            }

            OrdenTrabajo ordenGenerada = ordenRepo.saveAndFlush(orden);

            DetalleServicio servicioBase = new DetalleServicio();
            servicioBase.setOrdenTrabajo(ordenGenerada);
            servicioBase.setDescripcion("Servicio / Diagnóstico: " + cita.getMotivo());
            servicioBase.setCantidad(1);
            servicioBase.setHorasTecnicas(1.0);
            servicioBase.setPrecioUnitario(costoBase);
            detalleRepo.saveAndFlush(servicioBase);

            ordenInfo = new LinkedHashMap<>();
            ordenInfo.put("idOrden", ordenGenerada.getIdOrden());
            ordenInfo.put("codigoOrden", ordenGenerada.getCodigoOrden());
            ordenInfo.put("estado", ordenGenerada.getEstado());
        }

        registrarAuditoria("ORDENES", "Cambió cita #" + id + " (" + cita.getPlacaVehiculo() + ") a estado " + nuevoEstado
                + (ordenInfo != null ? " -> generó " + ordenInfo.get("codigoOrden") : ""), responsable);

        Map<String, Object> citaInfo = new LinkedHashMap<>();
        citaInfo.put("idCita", actualizada.getIdCita());
        citaInfo.put("estado", actualizada.getEstado());

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("cita", citaInfo);
        if (ordenInfo != null) {
            resp.put("ordenGenerada", ordenInfo);
        }
        return ResponseEntity.ok(resp);
    }

    @DeleteMapping("/citas/{id}")
    @Transactional
    public ResponseEntity<?> eliminarCita(@PathVariable Integer id, @RequestParam(defaultValue = "Recepción") String responsable) {
        CitaTaller cita = citaRepo.findById(id).orElseThrow(() -> new RuntimeException("Cita no encontrada"));
        String placa = cita.getPlacaVehiculo();
        citaRepo.delete(cita);
        registrarAuditoria("ORDENES", "Eliminó cita #" + id + " (" + placa + ")", responsable);
        return ResponseEntity.ok(Map.of("mensaje", "Cita #" + id + " eliminada."));
    }

    // 3. ÓRDENES DE TRABAJO
    @GetMapping("/ordenes")
    public List<OrdenTrabajo> listarOrdenes() {
        return ordenRepo.findAllByOrderByIdOrdenDesc();
    }

    @PostMapping("/ordenes")
    public ResponseEntity<OrdenTrabajo> crearOrden(@RequestBody Map<String, Object> payload) {
        String dni = payload.get("dni").toString().trim();
        String nombreCliente = payload.get("cliente").toString().trim();
        String telefono = payload.getOrDefault("telefono", "999999999").toString().trim();
        String emailCliente = payload.getOrDefault("email", "cliente@mecanix.com").toString().trim();
        String placa = payload.get("placa").toString().trim().toUpperCase();
        String marca = payload.get("marca").toString().trim();
        String modelo = payload.get("modelo").toString().trim();
        String falla = payload.get("falla").toString().trim();
        String infoAdicional = payload.getOrDefault("informacionAdicional", "").toString().trim();
        String responsable = payload.getOrDefault("responsable", "Recepción / Admin").toString();
        Double costo = Double.valueOf(payload.getOrDefault("costo", 150.0).toString());

        Cliente cliente = clienteRepo.findByDniRuc(dni).orElseGet(() -> {
            Cliente nuevo = new Cliente();
            nuevo.setDniRuc(dni);
            nuevo.setNombreCompleto(nombreCliente);
            nuevo.setTelefono(telefono);
            nuevo.setEmail(emailCliente);
            return clienteRepo.save(nuevo);
        });

        Vehiculo vehiculo = vehiculoRepo.findByPlacaIgnoreCase(placa).orElseGet(() -> {
            Vehiculo v = new Vehiculo();
            v.setPlaca(placa);
            v.setMarca(marca);
            v.setModelo(modelo);
            v.setAnio(2023);
            v.setCliente(cliente);
            return vehiculoRepo.save(v);
        });

        OrdenTrabajo orden = new OrdenTrabajo();
        orden.setCodigoOrden(generarCodigoOrdenUnico());
        orden.setDescripcionFalla(falla);
        orden.setInformacionAdicional(infoAdicional);
        orden.setCostoEstimado(costo);
        orden.setEstado("PENDIENTE");
        orden.setVehiculo(vehiculo);

        Object rawIdMec = payload.get("idMecanico");
        if (rawIdMec != null && !rawIdMec.toString().isBlank() && !"SIN_ASIGNAR".equalsIgnoreCase(rawIdMec.toString())) {
            usuarioRepo.findById(Integer.valueOf(rawIdMec.toString())).ifPresent(orden::setMecanicoAsignado);
        } else {
            orden.setMecanicoAsignado(null);
        }

        OrdenTrabajo guardada = ordenRepo.save(orden);

        DetalleServicio servicioBase = new DetalleServicio();
        servicioBase.setOrdenTrabajo(guardada);
        servicioBase.setDescripcion("Servicio / Diagnóstico: " + falla);
        servicioBase.setCantidad(1);
        servicioBase.setHorasTecnicas(1.0);
        servicioBase.setPrecioUnitario(costo);
        detalleRepo.save(servicioBase);

        String detalleMec = guardada.getMecanicoAsignado() != null
                ? ("asignada a " + guardada.getMecanicoAsignado().getNombreCompleto())
                : "en bolsa general";
        registrarAuditoria("ORDENES", "Creó orden " + guardada.getCodigoOrden() + " para placa " + placa + " (" + detalleMec + ")", responsable);
        return ResponseEntity.ok(guardada);
    }

    @PatchMapping("/ordenes/{id}/info-adicional")
    @Transactional
    public ResponseEntity<OrdenTrabajo> actualizarInfoAdicional(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        OrdenTrabajo orden = ordenRepo.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        String info = body.getOrDefault("informacionAdicional", "").trim();
        String responsable = body.getOrDefault("responsable", "Taller");
        orden.setInformacionAdicional(info);
        OrdenTrabajo guardada = ordenRepo.save(orden);
        registrarAuditoria("ORDENES", "Actualizó información adicional en " + orden.getCodigoOrden(), responsable);
        return ResponseEntity.ok(guardada);
    }

    @PatchMapping("/ordenes/{id}/asignar")
    @Transactional
    public ResponseEntity<OrdenTrabajo> asignarOrden(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        OrdenTrabajo orden = ordenRepo.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        String responsable = body.getOrDefault("responsable", "Taller").toString();

        Object rawIdMec = body.get("idMecanico");
        String emailMec = body.get("emailMecanico") != null ? body.get("emailMecanico").toString().trim() : "";

        if (rawIdMec == null || rawIdMec.toString().isBlank() || "SIN_ASIGNAR".equalsIgnoreCase(rawIdMec.toString())) {
            if (!emailMec.isBlank()) {
                Usuario mec = usuarioRepo.findByEmail(emailMec)
                        .orElseThrow(() -> new RuntimeException("Mecánico no encontrado"));
                orden.setMecanicoAsignado(mec);
                registrarAuditoria("ORDENES", "Mecánico " + mec.getNombreCompleto() + " tomó la orden " + orden.getCodigoOrden(), responsable);
            } else {
                orden.setMecanicoAsignado(null);
                registrarAuditoria("ORDENES", "Dejó orden " + orden.getCodigoOrden() + " disponible en bolsa general", responsable);
            }
        } else {
            Integer idMec = Integer.valueOf(rawIdMec.toString());
            Usuario mec = usuarioRepo.findById(idMec)
                    .orElseThrow(() -> new RuntimeException("Mecánico no encontrado"));
            orden.setMecanicoAsignado(mec);
            registrarAuditoria("ORDENES", "Asignó orden " + orden.getCodigoOrden() + " a " + mec.getNombreCompleto(), responsable);
        }

        return ResponseEntity.ok(ordenRepo.save(orden));
    }

    @PatchMapping("/ordenes/{id}/estado")
    public ResponseEntity<OrdenTrabajo> actualizarEstado(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        OrdenTrabajo orden = ordenRepo.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        String nuevoEstado = body.get("estado");
        String responsable = body.getOrDefault("responsable", "Taller");
        orden.setEstado(nuevoEstado);
        registrarAuditoria("ORDENES", "Cambió estado de " + orden.getCodigoOrden() + " a " + nuevoEstado, responsable);
        return ResponseEntity.ok(ordenRepo.save(orden));
    }

    @PatchMapping("/ordenes/{id}/confirmar-pago")
    @Transactional
    public ResponseEntity<OrdenTrabajo> confirmarPago(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        OrdenTrabajo orden = ordenRepo.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        String metodo = body.getOrDefault("metodoPago", "EFECTIVO").toUpperCase();
        String responsable = body.getOrDefault("responsable", "Caja / Cliente");

        orden.setEstadoPago("PAGADO");
        orden.setMetodoPago(metodo);
        orden.setFechaPago(LocalDateTime.now());
        if (!"LISTO".equals(orden.getEstado())) {
            orden.setEstado("LISTO");
        }

        OrdenTrabajo guardada = ordenRepo.save(orden);
        registrarAuditoria("DOCUMENTOS", "Confirmó pago (" + metodo + ") de " + orden.getCodigoOrden() + " por S/ " + String.format("%.2f", orden.getCostoEstimado() * 1.18), responsable);
        return ResponseEntity.ok(guardada);
    }

    @DeleteMapping("/ordenes/{id}")
    @Transactional
    public ResponseEntity<?> eliminarOrden(@PathVariable Integer id, @RequestParam(defaultValue = "Admin") String responsable) {
        OrdenTrabajo orden = ordenRepo.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));

        List<DetalleOrden> detalles = detalleRepo.findByOrdenTrabajo_IdOrden(id);
        for (DetalleOrden det : detalles) {
            if (det instanceof DetalleRepuesto detRep && detRep.getRepuesto() != null) {
                Repuesto rep = repuestoRepo.findById(detRep.getRepuesto().getIdRepuesto()).orElse(null);
                if (rep != null) {
                    rep.reponerStock(detRep.getCantidad());
                    repuestoRepo.save(rep);
                }
            }
            detalleRepo.delete(det);
        }

        List<DocumentoEmitido> docsAsociados = docRepo.findAll().stream()
                .filter(d -> d.getOrdenTrabajo() != null && id.equals(d.getOrdenTrabajo().getIdOrden()))
                .toList();
        for (DocumentoEmitido doc : docsAsociados) {
            docRepo.delete(doc);
        }

        List<MovimientoInventario> movsAsociados = movRepo.findAll().stream()
                .filter(m -> m.getOrdenTrabajo() != null && id.equals(m.getOrdenTrabajo().getIdOrden()))
                .toList();
        for (MovimientoInventario mov : movsAsociados) {
            mov.setOrdenTrabajo(null);
            movRepo.save(mov);
        }

        String codigo = orden.getCodigoOrden();
        ordenRepo.delete(orden);
        registrarAuditoria("ORDENES", "Eliminó orden " + codigo, responsable);
        return ResponseEntity.ok(Map.of("mensaje", "Orden " + codigo + " eliminada."));
    }

    // 4. DETALLES DE ORDEN
    @GetMapping("/ordenes/{id}/detalles")
    public Map<String, Object> obtenerDetallesOrden(@PathVariable Integer id) {
        OrdenTrabajo orden = ordenRepo.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        List<DetalleOrden> items = detalleRepo.findByOrdenTrabajo_IdOrden(id);

        double baseEstimada = orden.getCostoEstimado() != null ? orden.getCostoEstimado() : 0.0;
        double subtotal = items.isEmpty()
                ? baseEstimada
                : items.stream().mapToDouble(DetalleOrden::calcularSubtotal).sum();
        double igv = subtotal * 0.18;

        Map<String, Object> resp = new HashMap<>();
        resp.put("orden", orden);
        resp.put("items", items);
        resp.put("subtotal", subtotal);
        resp.put("igv", igv);
        resp.put("total", subtotal + igv);
        return resp;
    }

    @PostMapping("/ordenes/{id}/detalles")
    @Transactional
    public ResponseEntity<?> agregarDetalleOrden(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        OrdenTrabajo orden = ordenRepo.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        String tipo = body.getOrDefault("tipo", "REPUESTO").toString();
        String responsable = body.getOrDefault("responsable", "Mecánico").toString();

        if (detalleRepo.findByOrdenTrabajo_IdOrden(id).isEmpty()
                && orden.getCostoEstimado() != null && orden.getCostoEstimado() > 0) {
            DetalleServicio base = new DetalleServicio();
            base.setOrdenTrabajo(orden);
            base.setDescripcion("Servicio Base: " + orden.getDescripcionFalla());
            base.setCantidad(1);
            base.setHorasTecnicas(1.0);
            base.setPrecioUnitario(orden.getCostoEstimado());
            detalleRepo.save(base);
        }

        if ("REPUESTO".equalsIgnoreCase(tipo)) {
            Integer idRepuesto = Integer.valueOf(body.get("idRepuesto").toString());
            int cantidad = Integer.parseInt(body.get("cantidad").toString());
            if (cantidad <= 0) {
                return ResponseEntity.badRequest().body(Map.of("mensaje", "La cantidad debe ser mayor a 0."));
            }

            Repuesto repuesto = repuestoRepo.findById(idRepuesto)
                    .orElseThrow(() -> new RuntimeException("Repuesto no encontrado"));

            int stockAnterior = repuesto.getStock();
            repuesto.descontarStock(cantidad);
            repuestoRepo.save(repuesto);

            DetalleRepuesto detRep = new DetalleRepuesto();
            detRep.setOrdenTrabajo(orden);
            detRep.setRepuesto(repuesto);
            detRep.setDescripcion(repuesto.getNombre());
            detRep.setCantidad(cantidad);
            detRep.setPrecioUnitario(repuesto.getPrecioUnitario());
            detalleRepo.save(detRep);

            MovimientoInventario movUso = new MovimientoInventario(
                    repuesto, "USO_ORDEN", -cantidad, stockAnterior, repuesto.getStock(),
                    orden.getCodigoOrden(), "Pieza utilizada en " + orden.getVehiculo().getPlaca(), responsable
            );
            movUso.setOrdenTrabajo(orden);
            movRepo.save(movUso);

            registrarAuditoria("ALMACEN", "Usó " + cantidad + "x " + repuesto.getNombre() + " en " + orden.getCodigoOrden(), responsable);
        } else {
            String descripcion = body.get("descripcion").toString().trim();
            double horas = Double.parseDouble(body.getOrDefault("horas", 1.0).toString());
            double precio = Double.parseDouble(body.get("precio").toString());

            DetalleServicio detServ = new DetalleServicio();
            detServ.setOrdenTrabajo(orden);
            detServ.setDescripcion(descripcion);
            detServ.setCantidad(1);
            detServ.setHorasTecnicas(horas);
            detServ.setPrecioUnitario(precio);
            detalleRepo.save(detServ);

            registrarAuditoria("ORDENES", "Agregó mano de obra '" + descripcion + "' a " + orden.getCodigoOrden(), responsable);
        }

        double nuevoTotal = detalleRepo.findByOrdenTrabajo_IdOrden(id)
                .stream().mapToDouble(DetalleOrden::calcularSubtotal).sum();
        orden.setCostoEstimado(nuevoTotal);
        ordenRepo.save(orden);

        return ResponseEntity.ok(Map.of("mensaje", "Ítem agregado correctamente", "nuevoTotal", nuevoTotal));
    }

    @DeleteMapping("/ordenes/{idOrden}/detalles/{idDetalle}")
    @Transactional
    public ResponseEntity<?> eliminarDetalleOrden(
            @PathVariable Integer idOrden,
            @PathVariable Integer idDetalle,
            @RequestParam(defaultValue = "Taller") String responsable) {

        OrdenTrabajo orden = ordenRepo.findById(idOrden).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        DetalleOrden detalle = detalleRepo.findById(idDetalle).orElseThrow(() -> new RuntimeException("Ítem no encontrado"));

        if (detalle instanceof DetalleRepuesto detRep && detRep.getRepuesto() != null) {
            Repuesto rep = repuestoRepo.findById(detRep.getRepuesto().getIdRepuesto()).orElse(null);
            if (rep != null) {
                int stockAnterior = rep.getStock();
                rep.reponerStock(detRep.getCantidad());
                repuestoRepo.save(rep);

                MovimientoInventario movDev = new MovimientoInventario(
                        rep, "DEVOLUCION_ORDEN", detRep.getCantidad(), stockAnterior, rep.getStock(),
                        orden.getCodigoOrden(), "Corrección: pieza retirada de la orden", responsable
                );
                movDev.setOrdenTrabajo(orden);
                movRepo.save(movDev);
            }
        }

        detalleRepo.delete(detalle);

        double nuevoTotal = detalleRepo.findByOrdenTrabajo_IdOrden(idOrden)
                .stream().mapToDouble(DetalleOrden::calcularSubtotal).sum();
        orden.setCostoEstimado(nuevoTotal);
        ordenRepo.save(orden);

        registrarAuditoria("ALMACEN", "Retiró '" + detalle.getDescripcion() + "' de " + orden.getCodigoOrden() + " (stock devuelto)", responsable);
        return ResponseEntity.ok(Map.of("mensaje", "Ítem eliminado y stock restaurado.", "nuevoTotal", nuevoTotal));
    }

    // 5. INVENTARIO, AJUSTE MANUAL Y KARDEX
    @GetMapping("/repuestos")
    public List<Repuesto> listarRepuestos() {
        return repuestoRepo.findAll();
    }

    @PostMapping("/repuestos")
    @Transactional
    public ResponseEntity<Repuesto> crearRepuesto(@RequestBody Map<String, Object> body) {
        Repuesto rep = new Repuesto();
        rep.setCodigo(body.get("codigo").toString().trim().toUpperCase());
        rep.setNombre(body.get("nombre").toString().trim());
        rep.setPrecioUnitario(Double.valueOf(body.get("precioUnitario").toString()));
        rep.setStock(Integer.valueOf(body.get("stock").toString()));
        String responsable = body.getOrDefault("responsable", "Admin").toString();

        Repuesto guardado = repuestoRepo.save(rep);
        movRepo.save(new MovimientoInventario(
                guardado, "INGRESO_STOCK", guardado.getStock(), 0, guardado.getStock(),
                "-", "Registro inicial de repuesto en catálogo", responsable
        ));
        registrarAuditoria("ALMACEN", "Registró repuesto " + guardado.getCodigo() + " (" + guardado.getStock() + " und.)", responsable);
        return ResponseEntity.ok(guardado);
    }

    @PatchMapping("/repuestos/{id}/reponer")
    @Transactional
    public ResponseEntity<Repuesto> reponerStock(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Repuesto rep = repuestoRepo.findById(id).orElseThrow(() -> new RuntimeException("Repuesto no encontrado"));
        int cant = Integer.parseInt(body.getOrDefault("cantidad", 5).toString());
        String responsable = body.getOrDefault("responsable", "Admin").toString();

        int anterior = rep.getStock();
        rep.reponerStock(cant);
        repuestoRepo.save(rep);

        movRepo.save(new MovimientoInventario(
                rep, "INGRESO_STOCK", cant, anterior, rep.getStock(),
                "-", "Reposición rápida de almacén", responsable
        ));
        registrarAuditoria("ALMACEN", "Repuso +" + cant + " und. a " + rep.getNombre(), responsable);
        return ResponseEntity.ok(rep);
    }

    @PatchMapping("/repuestos/{id}/ajustar")
    @Transactional
    public ResponseEntity<?> ajustarStockManual(@PathVariable Integer id, @RequestBody Map<String, Object> body) {
        Repuesto rep = repuestoRepo.findById(id).orElseThrow(() -> new RuntimeException("Repuesto no encontrado"));
        String modo = body.getOrDefault("modo", "EXACTO").toString().toUpperCase();
        int valor = Integer.parseInt(body.get("valor").toString());
        String motivo = body.getOrDefault("motivo", "Ajuste manual por corrección").toString().trim();
        String responsable = body.getOrDefault("responsable", "Admin").toString();

        if (motivo.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Debes ingresar un motivo para el ajuste manual."));
        }

        int anterior = rep.getStock();
        int nuevoStock = switch (modo) {
            case "SUMAR" -> anterior + valor;
            case "RESTAR" -> anterior - valor;
            default -> valor;
        };

        rep.ajustarStockManual(nuevoStock);
        repuestoRepo.save(rep);

        int diferencia = nuevoStock - anterior;
        movRepo.save(new MovimientoInventario(
                rep, "AJUSTE_MANUAL", diferencia, anterior, nuevoStock,
                "-", motivo, responsable
        ));
        registrarAuditoria(
                "ALMACEN",
                "Ajuste manual en " + rep.getCodigo() + " (" + anterior + " -> " + nuevoStock + "): " + motivo,
                responsable
        );
        return ResponseEntity.ok(Map.of("mensaje", "Stock ajustado correctamente.", "repuesto", rep));
    }

    @GetMapping("/inventario/movimientos")
    public List<MovimientoInventario> listarMovimientosInventario(
            @RequestParam(required = false) Integer idRepuesto,
            @RequestParam(required = false, defaultValue = "TODOS") String tipo) {

        List<MovimientoInventario> lista = (idRepuesto != null)
                ? movRepo.findByRepuesto_IdRepuestoOrderByIdMovimientoDesc(idRepuesto)
                : movRepo.findAllByOrderByIdMovimientoDesc();

        if (tipo != null && !tipo.equalsIgnoreCase("TODOS") && !tipo.isBlank()) {
            return lista.stream().filter(m -> m.getTipoMovimiento().equalsIgnoreCase(tipo)).toList();
        }
        return lista;
    }

    @GetMapping("/inventario/movimientos/descargar")
    public ResponseEntity<byte[]> descargarMovimientosCsv(@RequestParam(defaultValue = "Admin") String responsable) {
        List<MovimientoInventario> movs = movRepo.findAllByOrderByIdMovimientoDesc();
        ExportadorCsv<MovimientoInventario> exportador = new ExportadorCsv<>();
        String[] encabezados = {"ID", "Codigo", "Repuesto", "Tipo", "Cantidad", "Stock Anterior", "Stock Nuevo", "Orden", "Motivo", "Responsable", "Fecha"};
        String csv = exportador.generarContenido(encabezados, movs);

        registrarAuditoria("ALMACEN", "Descargó Kardex CSV de movimientos de inventario (" + movs.size() + " registros)", responsable);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"kardex_inventario_" + LocalDate.now() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    // 6. DOCUMENTOS EMITIDOS
    @GetMapping("/documentos")
    public List<DocumentoEmitido> listarDocumentos() {
        return docRepo.findAllByOrderByIdDocumentoDesc();
    }

    @PostMapping("/documentos")
    public ResponseEntity<DocumentoEmitido> registrarDocumento(@RequestBody Map<String, Object> body) {
        String tipo = body.getOrDefault("tipo", "BOLETA").toString();
        String prefijo = tipo.equals("COTIZACION") ? "COT-2026-" : "BOL-2026-";

        DocumentoEmitido doc = new DocumentoEmitido();
        doc.setNumeroDocumento(prefijo + String.format("%04d", docRepo.count() + 1));
        doc.setTipoDocumento(tipo);
        doc.setClienteNombre(body.get("cliente").toString());
        doc.setPlacaVehiculo(body.get("placa").toString());
        doc.setMontoTotal(Double.valueOf(body.get("total").toString()));
        doc.setEmitidoPor(body.getOrDefault("emisor", "Administrador").toString());

        if (body.get("idOrden") != null) {
            Integer idOrden = Integer.valueOf(body.get("idOrden").toString());
            ordenRepo.findById(idOrden).ifPresent(doc::setOrdenTrabajo);
        }

        DocumentoEmitido guardado = docRepo.save(doc);
        registrarAuditoria("DOCUMENTOS", "Emitió " + guardado.getNumeroDocumento() + " (" + tipo + ")", doc.getEmitidoPor());
        return ResponseEntity.ok(guardado);
    }

    // 7. SEGURIDAD Y USUARIOS
    @GetMapping("/usuarios")
    public List<Usuario> listarUsuarios() {
        return usuarioRepo.findAll();
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuario(@RequestBody Map<String, String> body) {
        Rol rol = rolRepo.findByNombre(body.get("rol")).orElseThrow(() -> new RuntimeException("Rol no válido"));
        Usuario u = new Usuario();
        u.setNombreCompleto(body.get("nombre"));
        u.setEmail(body.get("email"));
        String claveInicial = body.getOrDefault("password", "123456");
        u.setPasswordHash(passwordEncoder.encode(claveInicial.isBlank() ? "123456" : claveInicial));
        u.setActivo(true);
        u.setRol(rol);
        registrarAuditoria("SEGURIDAD", "Creó usuario " + u.getEmail() + " (" + rol.getNombre() + ")", "Admin");
        return ResponseEntity.ok(usuarioRepo.save(u));
    }

    @PatchMapping("/usuarios/{id}/reset-password")
    public ResponseEntity<?> resetPassword(@PathVariable Integer id, @RequestBody Map<String, String> body) {
        Usuario u = usuarioRepo.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        String nuevaClave = body.getOrDefault("nuevaClave", "123456");
        u.setPasswordHash(passwordEncoder.encode(nuevaClave));
        usuarioRepo.save(u);
        registrarAuditoria("SEGURIDAD", "Restableció contraseña de " + u.getEmail(), "Admin");
        return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada correctamente para " + u.getEmail()));
    }

    @PatchMapping("/mi-clave")
    public ResponseEntity<?> cambiarMiClave(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String claveActual = body.get("claveActual");
        String claveNueva = body.get("claveNueva");

        if (claveNueva == null || claveNueva.trim().length() < 4) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La nueva contraseña debe tener al menos 4 caracteres."));
        }

        Usuario u = usuarioRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!passwordEncoder.matches(claveActual, u.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "La contraseña actual es incorrecta."));
        }

        u.setPasswordHash(passwordEncoder.encode(claveNueva.trim()));
        usuarioRepo.save(u);
        registrarAuditoria("SEGURIDAD", "Cambió su propia contraseña (" + u.getEmail() + ")", u.getNombreCompleto());
        return ResponseEntity.ok(Map.of("mensaje", "Tu contraseña se actualizó correctamente."));
    }

    @PatchMapping("/usuarios/{id}/toggle-estado")
    public ResponseEntity<?> toggleEstadoUsuario(@PathVariable Integer id) {
        Usuario u = usuarioRepo.findById(id).orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if ("ADMIN".equals(u.getRol().getNombre()) && Boolean.TRUE.equals(u.getActivo())) {
            long adminsActivos = usuarioRepo.findAll().stream()
                    .filter(usr -> "ADMIN".equals(usr.getRol().getNombre()) && Boolean.TRUE.equals(usr.getActivo()))
                    .count();
            if (adminsActivos <= 1) {
                return ResponseEntity.badRequest()
                        .body(Map.of("mensaje", "Operación denegada: debe quedar al menos un Administrador activo."));
            }
        }
        boolean nuevoEstado = !Boolean.TRUE.equals(u.getActivo());
        u.setActivo(nuevoEstado);
        usuarioRepo.save(u);
        registrarAuditoria("SEGURIDAD", (nuevoEstado ? "Activó" : "Bloqueó") + " acceso a " + u.getEmail(), "Admin");
        return ResponseEntity.ok(u);
    }

    // 8. AUDITORÍA POR INTERVALOS Y DESCARGA CSV
    @GetMapping("/auditoria")
    public List<AuditoriaLog> listarAuditoria(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false, defaultValue = "TODOS") String modulo) {
        return consultarLogsPorIntervalo(desde, hasta, modulo);
    }

    @GetMapping("/auditoria/descargar")
    public ResponseEntity<byte[]> descargarAuditoriaCsv(
            @RequestParam(required = false) String desde,
            @RequestParam(required = false) String hasta,
            @RequestParam(required = false, defaultValue = "TODOS") String modulo,
            @RequestParam(required = false, defaultValue = "Admin") String responsable) {

        List<AuditoriaLog> logs = new ArrayList<>(consultarLogsPorIntervalo(desde, hasta, modulo));
        String rangoTexto = (desde != null && !desde.isBlank() ? desde : "Inicio")
                + " al " + (hasta != null && !hasta.isBlank() ? hasta : "Hoy");

        AuditoriaLog logDescarga = registrarAuditoria(
                "AUDITORIA",
                "Descargó reporte CSV [" + modulo + "] del " + rangoTexto + " (" + logs.size() + " registros)",
                responsable
        );
        logs.add(0, logDescarga);

        ExportadorCsv<AuditoriaLog> exportador = new ExportadorCsv<>();
        String[] encabezados = {"ID", "Modulo", "Accion Registrada", "Responsable", "Fecha y Hora"};
        String contenidoCsv = exportador.generarContenido(encabezados, logs);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"auditoria_mecanix_" + LocalDate.now() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(contenidoCsv.getBytes(StandardCharsets.UTF_8));
    }

    private List<AuditoriaLog> consultarLogsPorIntervalo(String desde, String hasta, String modulo) {
        LocalDateTime inicio = (desde != null && !desde.isBlank())
                ? LocalDate.parse(desde.trim()).atStartOfDay()
                : LocalDateTime.of(2020, 1, 1, 0, 0, 0);

        LocalDateTime fin = (hasta != null && !hasta.isBlank())
                ? LocalDate.parse(hasta.trim()).atTime(LocalTime.of(23, 59, 59))
                : LocalDateTime.now().plusDays(1);

        if (modulo != null && !modulo.equalsIgnoreCase("TODOS") && !modulo.isBlank()) {
            return auditRepo.findByModuloAndFechaHoraBetweenOrderByFechaHoraDesc(modulo.toUpperCase(), inicio, fin);
        }
        return auditRepo.findByFechaHoraBetweenOrderByFechaHoraDesc(inicio, fin);
    }

    // 9. CLIENTES Y VEHÍCULOS
    @GetMapping("/clientes")
    public List<Cliente> listarClientes() { return clienteRepo.findAll(); }

    @GetMapping("/vehiculos")
    public List<Vehiculo> listarVehiculos() { return vehiculoRepo.findAll(); }

    // 10. HISTORIAL DE TRABAJOS Y GANANCIAS POR MECÁNICO
    public static final class TrabajoMecanico {
        private final Integer idOrden;
        private final String codigoOrden;
        private final LocalDateTime fechaIngreso;
        private final String placa;
        private final String vehiculo;
        private final String descripcionFalla;
        private final String estado;
        private final double montoActividad;
        private final double gananciaActividad;

        public TrabajoMecanico(Integer idOrden, String codigoOrden, LocalDateTime fechaIngreso,
                               String placa, String vehiculo, String descripcionFalla,
                               String estado, double montoActividad, double gananciaActividad) {
            this.idOrden = idOrden;
            this.codigoOrden = codigoOrden;
            this.fechaIngreso = fechaIngreso;
            this.placa = placa;
            this.vehiculo = vehiculo;
            this.descripcionFalla = descripcionFalla;
            this.estado = estado;
            this.montoActividad = montoActividad;
            this.gananciaActividad = gananciaActividad;
        }

        public Integer getIdOrden() { return idOrden; }
        public String getCodigoOrden() { return codigoOrden; }
        public LocalDateTime getFechaIngreso() { return fechaIngreso; }
        public String getPlaca() { return placa; }
        public String getVehiculo() { return vehiculo; }
        public String getDescripcionFalla() { return descripcionFalla; }
        public String getEstado() { return estado; }
        public double getMontoActividad() { return montoActividad; }
        public double getGananciaActividad() { return gananciaActividad; }
    }

    public static final class FilaMecanico {
        private final Integer idUsuario;
        private final String nombreCompleto;
        private final String email;
        private final Boolean activo;
        private final int totalOrdenes;
        private final int trabajosRealizados;
        private final int enProceso;
        private final double montoGenerado;
        private final double gananciaActividad;
        private final List<TrabajoMecanico> trabajos;

        public FilaMecanico(Integer idUsuario, String nombreCompleto, String email, Boolean activo,
                            int totalOrdenes, int trabajosRealizados, int enProceso,
                            double montoGenerado, double gananciaActividad, List<TrabajoMecanico> trabajos) {
            this.idUsuario = idUsuario;
            this.nombreCompleto = nombreCompleto;
            this.email = email;
            this.activo = activo;
            this.totalOrdenes = totalOrdenes;
            this.trabajosRealizados = trabajosRealizados;
            this.enProceso = enProceso;
            this.montoGenerado = montoGenerado;
            this.gananciaActividad = gananciaActividad;
            this.trabajos = trabajos;
        }

        public Integer getIdUsuario() { return idUsuario; }
        public String getNombreCompleto() { return nombreCompleto; }
        public String getEmail() { return email; }
        public Boolean getActivo() { return activo; }
        public int getTotalOrdenes() { return totalOrdenes; }
        public int getTrabajosRealizados() { return trabajosRealizados; }
        public int getEnProceso() { return enProceso; }
        public double getMontoGenerado() { return montoGenerado; }
        public double getGananciaActividad() { return gananciaActividad; }
        public List<TrabajoMecanico> getTrabajos() { return trabajos; }
    }

    @GetMapping("/mecanicos/historial")
    public Map<String, Object> obtenerHistorialMecanicos(
            @RequestParam(required = false) String rol,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String nombre) {

        List<Usuario> todosUsuarios = usuarioRepo.findAll();
        List<Usuario> mecanicos = new ArrayList<>();
        for (Usuario u : todosUsuarios) {
            if (u.getRol() != null && "MECANICO".equalsIgnoreCase(u.getRol().getNombre())) {
                mecanicos.add(u);
            }
        }

        List<OrdenTrabajo> todasOrdenes = ordenRepo.findAllByOrderByIdOrdenDesc();
        double porcentajeComision = 0.35;

        List<FilaMecanico> resumenMecanicos = new ArrayList<>();

        for (Usuario mec : mecanicos) {
            List<OrdenTrabajo> asignadas = new ArrayList<>();
            List<OrdenTrabajo> realizados = new ArrayList<>();
            int enProceso = 0;
            double montoGenerado = 0.0;

            for (OrdenTrabajo o : todasOrdenes) {
                if (o.getMecanicoAsignado() != null
                        && o.getMecanicoAsignado().getIdUsuario() != null
                        && o.getMecanicoAsignado().getIdUsuario().equals(mec.getIdUsuario())) {
                    asignadas.add(o);
                    if ("LISTO".equalsIgnoreCase(o.getEstado())) {
                        realizados.add(o);
                        double costo = o.getCostoEstimado() != null ? o.getCostoEstimado() : 0.0;
                        montoGenerado += costo;
                    } else {
                        enProceso++;
                    }
                }
            }

            montoGenerado = Math.round(montoGenerado * 100.0) / 100.0;
            double gananciaActividad = Math.round(montoGenerado * porcentajeComision * 100.0) / 100.0;

            List<TrabajoMecanico> detalleTrabajos = new ArrayList<>();
            for (OrdenTrabajo o : realizados) {
                double monto = o.getCostoEstimado() != null ? o.getCostoEstimado() : 0.0;
                double gan = Math.round(monto * porcentajeComision * 100.0) / 100.0;
                String placa = o.getVehiculo() != null ? o.getVehiculo().getPlaca() : "-";
                String vehiculo = o.getVehiculo() != null
                        ? (o.getVehiculo().getMarca() + " " + o.getVehiculo().getModelo()) : "-";

                detalleTrabajos.add(new TrabajoMecanico(
                        o.getIdOrden(), o.getCodigoOrden(), o.getFechaIngreso(),
                        placa, vehiculo, o.getDescripcionFalla(), o.getEstado(), monto, gan
                ));
            }

            resumenMecanicos.add(new FilaMecanico(
                    mec.getIdUsuario(), mec.getNombreCompleto(), mec.getEmail(), mec.getActivo(),
                    asignadas.size(), realizados.size(), enProceso, montoGenerado, gananciaActividad, detalleTrabajos
            ));
        }

        Map<String, Object> respuesta = new LinkedHashMap<>();
        respuesta.put("porcentajeGanancia", porcentajeComision * 100);

        if ("MECANICO".equalsIgnoreCase(rol)) {
            FilaMecanico propio = null;
            for (FilaMecanico m : resumenMecanicos) {
                boolean matchEmail = email != null && !email.isBlank()
                        && m.getEmail() != null && email.trim().equalsIgnoreCase(m.getEmail().trim());
                boolean matchNombre = nombre != null && !nombre.isBlank()
                        && m.getNombreCompleto() != null && nombre.trim().equalsIgnoreCase(m.getNombreCompleto().trim());
                if (matchEmail || matchNombre) {
                    propio = m;
                    break;
                }
            }
            if (propio == null) {
                propio = new FilaMecanico(
                        null, nombre != null ? nombre : "Mecánico", email != null ? email : "",
                        true, 0, 0, 0, 0.0, 0.0, new ArrayList<>()
                );
            }
            respuesta.put("mecanicoActual", propio);
        } else {
            respuesta.put("mecanicos", resumenMecanicos);
        }

        return respuesta;
    }
}