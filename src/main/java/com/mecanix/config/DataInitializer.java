package com.mecanix.config;

import com.mecanix.model.*;
import com.mecanix.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final VehiculoRepository vehiculoRepository;
    private final RepuestoRepository repuestoRepository;
    private final OrdenTrabajoRepository ordenTrabajoRepository;
    private final DetalleOrdenRepository detalleOrdenRepository;
    private final MovimientoInventarioRepository movimientoInventarioRepository;
    private final DocumentoEmitidoRepository documentoEmitidoRepository;
    private final AuditoriaLogRepository auditoriaLogRepository;
    private final CitaTallerRepository citaTallerRepository;
    private final PasswordEncoder passwordEncoder;

    private static final LocalDate FECHA_INICIO_RANGO = LocalDate.of(2026, 7, 1);
    private static final LocalDate FECHA_FIN_RANGO = LocalDate.of(2026, 11, 20);

    public DataInitializer(RolRepository rolRepository,
                           UsuarioRepository usuarioRepository,
                           ClienteRepository clienteRepository,
                           VehiculoRepository vehiculoRepository,
                           RepuestoRepository repuestoRepository,
                           OrdenTrabajoRepository ordenTrabajoRepository,
                           DetalleOrdenRepository detalleOrdenRepository,
                           MovimientoInventarioRepository movimientoInventarioRepository,
                           DocumentoEmitidoRepository documentoEmitidoRepository,
                           AuditoriaLogRepository auditoriaLogRepository,
                           CitaTallerRepository citaTallerRepository,
                           PasswordEncoder passwordEncoder) {
        this.rolRepository = rolRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.vehiculoRepository = vehiculoRepository;
        this.repuestoRepository = repuestoRepository;
        this.ordenTrabajoRepository = ordenTrabajoRepository;
        this.detalleOrdenRepository = detalleOrdenRepository;
        this.movimientoInventarioRepository = movimientoInventarioRepository;
        this.documentoEmitidoRepository = documentoEmitidoRepository;
        this.auditoriaLogRepository = auditoriaLogRepository;
        this.citaTallerRepository = citaTallerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private LocalDateTime fechaEnRango(int indice, int total, int hora, int minuto) {
        long diasTotales = ChronoUnit.DAYS.between(FECHA_INICIO_RANGO, FECHA_FIN_RANGO);
        long offsetDias = (total <= 1) ? 0 : Math.min(diasTotales, (long) indice * diasTotales / (total - 1));
        LocalDate fecha = FECHA_INICIO_RANGO.plusDays(offsetDias);
        return LocalDateTime.of(fecha, LocalTime.of(hora % 24, minuto % 60));
    }

    @Override
    @Transactional
    public void run(String... args) {
        String claveHashPorDefecto = passwordEncoder.encode("123456");
        Random rnd = new Random(2026);

        List<Usuario> mecanicos = new ArrayList<>();
        List<Usuario> recepcionistas = new ArrayList<>();

        if (rolRepository.count() == 0) {
            Rol adminRol = crearRol("ADMIN");
            Rol recepRol = crearRol("RECEPCIONISTA");
            Rol mecRol = crearRol("MECANICO");
            Rol cliRol = crearRol("CLIENTE");

            crearUsuario("Steven Alpízar", "admin@mecanix.com", claveHashPorDefecto, adminRol, true);
            crearUsuario("Valeria Montoya", "admin2@mecanix.com", claveHashPorDefecto, adminRol, true);

            recepcionistas.add(crearUsuario("Camila Torres", "recepcion@mecanix.com", claveHashPorDefecto, recepRol, true));
            recepcionistas.add(crearUsuario("Sofía Benavides", "recepcion2@mecanix.com", claveHashPorDefecto, recepRol, true));
            recepcionistas.add(crearUsuario("Diego Salazar", "recepcion3@mecanix.com", claveHashPorDefecto, recepRol, true));
            recepcionistas.add(crearUsuario("Paola Cárdenas", "recepcion4@mecanix.com", claveHashPorDefecto, recepRol, true));

            String[][] listaMecanicos = {
                {"Carlos Mendoza", "mecanico@mecanix.com"},
                {"Lucía Paredes", "mecanico2@mecanix.com"},
                {"Héctor Ramírez", "h.ramirez@mecanix.com"},
                {"Marco Antonio Silva", "m.silva@mecanix.com"},
                {"Renzo Huamán", "r.huaman@mecanix.com"},
                {"José Luis Vargas", "j.vargas@mecanix.com"},
                {"Fernando Castillo", "f.castillo@mecanix.com"},
                {"Ricardo Espinoza", "r.espinoza@mecanix.com"},
                {"Miguel Ángel Rojas", "m.rojas@mecanix.com"}
            };
            for (int i = 0; i < listaMecanicos.length; i++) {
                mecanicos.add(crearUsuario(listaMecanicos[i][0], listaMecanicos[i][1], claveHashPorDefecto, mecRol, i != 8));
            }

            crearUsuario("Walter Quesada", "cliente@mecanix.com", claveHashPorDefecto, cliRol, true);
            crearUsuario("Transportes Sur Andino SAC", "flota@surandino.pe", claveHashPorDefecto, cliRol, true);
            crearUsuario("Agroexportadora Valle de Ica SRL", "logistica@valledeica.pe", claveHashPorDefecto, cliRol, true);
        }

        List<Repuesto> catalogoRepuestos = new ArrayList<>();
        if (repuestoRepository.count() == 0) {
            Object[][] repuestosBase = {
                {"REP-001", "Filtro de Aceite Sintético 5W-30", 48.0, 45},
                {"REP-002", "Pastillas de Freno Delanteras Cerámicas", 145.0, 20},
                {"REP-003", "Bujías Iridium (Set x4)", 115.0, 35},
                {"REP-004", "Aceite Motor Full Sintético 5W-30 (Galón)", 165.0, 40},
                {"REP-005", "Filtro de Aire Acondicionado / Cabina", 38.0, 18},
                {"REP-006", "Kit de Embrague / Disco y Plato", 420.0, 22},
                {"REP-007", "Batería 12V 70Ah Libre Mantenimiento", 310.0, 25},
                {"REP-008", "Amortiguador Delantero Reforzado", 195.0, 16},
                {"REP-009", "Filtro de Combustible Diésel / Gasolina", 62.0, 32},
                {"REP-010", "Correa de Distribución + Tensor", 240.0, 24},
                {"REP-011", "Discos de Freno Ventilados (Par)", 280.0, 20},
                {"REP-012", "Refrigerante Orgánico 50/50 (Galón)", 55.0, 48},
                {"REP-013", "Líquido de Frenos DOT-4 (500ml)", 32.0, 38},
                {"REP-014", "Rótula de Suspensión Inferior", 88.0, 28},
                {"REP-015", "Terminal de Dirección Exterior", 75.0, 30},
                {"REP-016", "Bomba de Agua con Empaque", 185.0, 20},
                {"REP-017", "Sensor de Oxígeno Lambda Universal", 210.0, 18},
                {"REP-018", "Bobina de Encendido Individual", 135.0, 26},
                {"REP-019", "Filtro de Aire de Motor Alto Flujo", 52.0, 36},
                {"REP-020", "Rodamiento de Maza Delantera con ABS", 160.0, 22},
                {"REP-021", "Alternador 12V 110A Reconstruido", 520.0, 15},
                {"REP-022", "Termostato con Carcasa", 92.0, 28},
                {"REP-023", "Faja de Accesorios Poly-V", 68.0, 34},
                {"REP-024", "Juego de Escobillas Limpiaparabrisas", 42.0, 40},
                {"REP-025", "Aceite de Transmisión Automática ATF (Litro)", 58.0, 38}
            };

            LocalDateTime fechaBaseAlmacen = LocalDateTime.of(FECHA_INICIO_RANGO, LocalTime.of(8, 30));
            for (Object[] r : repuestosBase) {
                Repuesto rep = new Repuesto();
                rep.setCodigo((String) r[0]);
                rep.setNombre((String) r[1]);
                rep.setPrecioUnitario((Double) r[2]);
                rep.setStock((Integer) r[3]);
                Repuesto guardado = repuestoRepository.save(rep);
                catalogoRepuestos.add(guardado);

                MovimientoInventario movInit = new MovimientoInventario(
                        guardado, "INGRESO_STOCK", guardado.getStock(), 0, guardado.getStock(),
                        "-", "Ingreso inicial de stock en almacén", "Steven Alpízar"
                );
                movInit.setFechaHora(fechaBaseAlmacen);
                movimientoInventarioRepository.save(movInit);
            }
        } else {
            catalogoRepuestos = repuestoRepository.findAll();
        }

        List<Vehiculo> flotaVehiculos = new ArrayList<>();
        if (clienteRepository.count() == 0) {
            String[] nombres = {
                "Walter", "Andrea", "Jorge", "María", "Luis", "Carmen", "Pedro", "Rosa", "Javier", "Elena",
                "Roberto", "Patricia", "Manuel", "Gabriela", "Alejandro", "Claudia", "Daniel", "Verónica", "Eduardo", "Natalia",
                "Raúl", "Mónica", "Arturo", "Silvia", "Oscar", "Lorena", "Guillermo", "Beatriz", "César", "Diana",
                "Enrique", "Karina", "Hugo", "Roxana", "Iván", "Fiorella", "Julio", "Milagros", "Martín", "Cynthia",
                "Sergio", "Vanessa", "Víctor", "Marisol", "Alberto", "Sandra", "Ernesto", "Jessica", "Gonzalo", "Pamela"
            };
            String[] apellidos = {
                "Quesada", "Salgado", "Quispe", "Huamán", "Mendoza", "Flores", "Chávez", "Ríos", "Paredes", "Gutiérrez",
                "Cárdenas", "Vargas", "Castillo", "Espinoza", "Rojas", "Torres", "Díaz", "Morales", "Reyes", "Cruz",
                "Ortiz", "Silva", "Ramos", "Pérez", "Sánchez", "Romero", "Herrera", "Medina", "Aguilar", "Vega",
                "Castro", "Campos", "Pacheco", "Miranda", "Navarro", "Delgado", "León", "Salazar", "Palomino", "Zevallos"
            };
            String[][] empresasFlota = {
                {"20601948271", "Transportes Sur Andino SAC", "flota@surandino.pe"},
                {"20541982310", "Agroexportadora Valle de Ica SRL", "logistica@valledeica.pe"},
                {"20491827364", "Distribuidora Comercial Paracas EIRL", "flota@paracas.pe"},
                {"20608877665", "Constructora e Ingeniería Nazca SAC", "equipos@ingenazca.pe"},
                {"20512398741", "Servicios Logísticos Huacachina SA", "operaciones@huacachina.pe"},
                {"20609911223", "Agroindustrias Sol del Sur SAC", "taller@soldelsur.pe"},
                {"20456789123", "Transportes Rápido Chincha SRL", "mantenimiento@rapidochincha.pe"},
                {"20554433221", "Operador Turístico Dunas Express SAC", "unidades@dunasexpress.pe"},
                {"20603322114", "Ferretería y Maquinarias Pisco SAC", "flota@piscoferre.pe"},
                {"20587654321", "Distribuidora Bebidas del Sur SA", "reparto@bebidasur.pe"}
            };
            String[][] modelosAuto = {
                {"Toyota", "Hilux 4x4 SRV"}, {"Toyota", "Corolla 1.8"}, {"Toyota", "Fortuner 2.8"}, {"Toyota", "Yaris Sedan"},
                {"Hyundai", "Tucson 2.0"}, {"Hyundai", "Accent GL"}, {"Hyundai", "Santa Fe"}, {"Hyundai", "H-1 Panel"},
                {"Kia", "Sportage GT-Line"}, {"Kia", "Rio Sedan"}, {"Kia", "Sorento EX"}, {"Kia", "Frontier K2700"},
                {"Nissan", "Frontier NP300"}, {"Nissan", "Sentra 2.0"}, {"Nissan", "X-Trail"}, {"Nissan", "Versa"},
                {"Mitsubishi", "L200 GLS"}, {"Mitsubishi", "Montero Sport"}, {"Chevrolet", "N400 Max"}, {"Mazda", "CX-5"}
            };

            int seqPlaca = 100;
            for (int e = 0; e < empresasFlota.length; e++) {
                Cliente emp = crearCliente(empresasFlota[e][0], empresasFlota[e][1], "987" + String.format("%06d", 100000 + e), empresasFlota[e][2]);
                for (int v = 0; v < 3; v++) {
                    String[] mod = modelosAuto[(e * 3 + v) % modelosAuto.length];
                    String placa = (e == 0 && v == 0) ? "D4F-782" : String.format("F%dK-%03d", (e % 9) + 1, seqPlaca++);
                    flotaVehiculos.add(crearVehiculo(placa, mod[0], mod[1], 2020 + (v % 5), emp));
                }
            }

            for (int i = 0; i < 90; i++) {
                String nom = (i == 0)
                        ? "Walter Quesada"
                        : nombres[i % nombres.length] + " " + apellidos[(i * 3) % apellidos.length] + " " + apellidos[(i * 7 + 1) % apellidos.length];
                String email = (i == 0) ? "cliente@mecanix.com" : "cliente" + (i + 1) + "@correo.com";
                String dni = (i == 0) ? "72849102" : String.format("%08d", 41200000 + i * 137);
                String tel = "9" + String.format("%08d", 50000000 + i * 293);

                Cliente cli = crearCliente(dni, nom, tel, email);
                String[] mod = modelosAuto[i % modelosAuto.length];
                String placa = (i == 0) ? "ABC-123" : String.format("%c%d%c-%03d", (char) ('A' + (i % 24)), (i % 9) + 1, (char) ('B' + ((i * 2) % 23)), seqPlaca++);
                flotaVehiculos.add(crearVehiculo(placa, mod[0], mod[1], 2018 + (i % 8), cli));
            }
        } else {
            flotaVehiculos = vehiculoRepository.findAll();
        }

        if (mecanicos.isEmpty()) {
            mecanicos = usuarioRepository.findAll().stream()
                    .filter(u -> u.getRol() != null && "MECANICO".equals(u.getRol().getNombre()))
                    .toList();
        }
        if (recepcionistas.isEmpty()) {
            recepcionistas = usuarioRepository.findAll().stream()
                    .filter(u -> u.getRol() != null && ("RECEPCIONISTA".equals(u.getRol().getNombre()) || "ADMIN".equals(u.getRol().getNombre())))
                    .toList();
        }

        if (ordenTrabajoRepository.count() == 0 && !flotaVehiculos.isEmpty()) {
            String[] fallasComunes = {
                "Mantenimiento preventivo 10,000 km + cambio de aceite y filtro",
                "Revisión de sistema de frenos y cambio de pastillas delanteras",
                "Diagnóstico computarizado por testigo Check Engine encendido",
                "Cambio de kit de embrague y rectificado de volante",
                "Ruido en suspensión delantera, revisión de rótulas y amortiguadores",
                "Afinamiento electrónico completo, limpieza de inyectores y cambio de bujías",
                "Cambio de correa de distribución, tensor y bomba de agua",
                "Falla en sistema de carga, revisión de alternador y batería",
                "Mantenimiento de 40,000 km para unidad de flota operativa",
                "Recalentamiento de motor, cambio de termostato y refrigerante"
            };
            String[] estados = {"LISTO", "LISTO", "LISTO", "EN_REPARACION", "EN_DIAGNOSTICO", "PENDIENTE"};

            int totalOrdenes = 140;
            int docSeq = 1;
            for (int idx = 1; idx <= totalOrdenes; idx++) {
                Vehiculo v = flotaVehiculos.get((idx - 1) % flotaVehiculos.size());
                Usuario mec = mecanicos.isEmpty() ? null : mecanicos.get(idx % mecanicos.size());
                Usuario emisor = recepcionistas.isEmpty() ? null : recepcionistas.get(idx % recepcionistas.size());
                String estado = (idx <= 8) ? estados[idx % estados.length] : estados[(idx + rnd.nextInt(3)) % estados.length];
                String falla = fallasComunes[idx % fallasComunes.length];
                double manoObra = 120.0 + (idx % 6) * 35.0;
                LocalDateTime fechaOt = fechaEnRango(idx - 1, totalOrdenes, 8 + (idx % 10), (idx * 7) % 60);

                OrdenTrabajo ot = new OrdenTrabajo();
                ot.setCodigoOrden("OT-2026-" + String.format("%04d", idx));
                ot.setDescripcionFalla(falla);
                ot.setEstado(estado);
                ot.setVehiculo(v);
                ot.setMecanicoAsignado(mec);
                ot.setFechaIngreso(fechaOt);
                ot.setCostoEstimado(manoObra);
                OrdenTrabajo guardada = ordenTrabajoRepository.save(ot);

                DetalleServicio ds = new DetalleServicio();
                ds.setOrdenTrabajo(guardada);
                ds.setDescripcion("Servicio Técnico: " + falla);
                ds.setCantidad(1);
                ds.setHorasTecnicas(1.5 + (idx % 3) * 0.5);
                ds.setPrecioUnitario(manoObra);
                detalleOrdenRepository.save(ds);

                double totalOrden = manoObra;

                if (idx % 3 != 0 && !catalogoRepuestos.isEmpty()) {
                    Repuesto rep = catalogoRepuestos.get((idx * 3) % catalogoRepuestos.size());
                    int cantUsada = (idx % 2 == 0) ? 1 : 2;

                    if (rep.getStock() >= cantUsada) {
                        int stockAnt = rep.getStock();
                        rep.descontarStock(cantUsada);
                        repuestoRepository.save(rep);

                        detalleOrdenRepository.save(crearDetalleRepuesto(guardada, rep, cantUsada));
                        totalOrden += rep.getPrecioUnitario() * cantUsada;

                        MovimientoInventario mov = new MovimientoInventario(
                                rep, "USO_ORDEN", -cantUsada, stockAnt, rep.getStock(),
                                guardada.getCodigoOrden(), "Repuesto instalado en placa " + v.getPlaca(),
                                mec != null ? mec.getNombreCompleto() : "Carlos Mendoza"
                        );
                        mov.setOrdenTrabajo(guardada);
                        mov.setFechaHora(fechaOt.plusHours(1));
                        movimientoInventarioRepository.save(mov);
                    }
                }

                guardada.setCostoEstimado(totalOrden);
                ordenTrabajoRepository.save(guardada);

                if ("LISTO".equals(estado)) {
                    DocumentoEmitido bol = new DocumentoEmitido();
                    bol.setNumeroDocumento("BOL-2026-" + String.format("%04d", docSeq++));
                    bol.setTipoDocumento("BOLETA");
                    bol.setClienteNombre(v.getCliente().getNombreCompleto());
                    bol.setPlacaVehiculo(v.getPlaca());
                    bol.setMontoTotal(Math.round(totalOrden * 1.18 * 100.0) / 100.0);
                    bol.setEmitidoPor(emisor != null ? emisor.getNombreCompleto() : "Camila Torres");
                    bol.setFechaEmision(fechaOt.plusHours(3));
                    bol.setOrdenTrabajo(guardada);
                    documentoEmitidoRepository.save(bol);
                } else if ("PENDIENTE".equals(estado) && idx % 2 == 0) {
                    DocumentoEmitido cot = new DocumentoEmitido();
                    cot.setNumeroDocumento("COT-2026-" + String.format("%04d", docSeq++));
                    cot.setTipoDocumento("COTIZACION");
                    cot.setClienteNombre(v.getCliente().getNombreCompleto());
                    cot.setPlacaVehiculo(v.getPlaca());
                    cot.setMontoTotal(Math.round(totalOrden * 1.18 * 100.0) / 100.0);
                    cot.setEmitidoPor(emisor != null ? emisor.getNombreCompleto() : "Camila Torres");
                    cot.setFechaEmision(fechaOt.plusMinutes(25));
                    cot.setOrdenTrabajo(guardada);
                    documentoEmitidoRepository.save(cot);
                }
            }

            String[] modulosAud = {"ORDENES", "ALMACEN", "DOCUMENTOS", "SEGURIDAD", "AUDITORIA"};
            String[] accionesAud = {
                "Registró ingreso de unidad vehicular y generó orden de trabajo",
                "Descontó repuestos del almacén central para orden en curso",
                "Emitió comprobante electrónico y archivó respaldo",
                "Actualizó estado técnico de diagnóstico a reparación",
                "Verificó stock mínimo y reposición de filtros",
                "Validó credenciales de acceso y permisos de rol"
            };
            int totalLogs = 50;
            for (int a = 1; a <= totalLogs; a++) {
                Usuario actor = recepcionistas.isEmpty() ? null : recepcionistas.get(a % recepcionistas.size());
                AuditoriaLog log = new AuditoriaLog(
                        modulosAud[a % modulosAud.length],
                        accionesAud[a % accionesAud.length] + " (Registro #" + a + ")",
                        actor != null ? actor.getNombreCompleto() : "Camila Torres"
                );
                log.setUsuario(actor);
                log.setFechaHora(fechaEnRango(a - 1, totalLogs, 9 + (a % 9), (a * 11) % 60));
                auditoriaLogRepository.save(log);
            }
        }

        if (citaTallerRepository.count() == 0 && !flotaVehiculos.isEmpty()) {
            String[] motivosCita = {
                "Mantenimiento preventivo 10,000 km",
                "Diagnóstico computarizado / Scanner",
                "Revisión de frenos y pastillas",
                "Suspensión, rótulas y alineamiento",
                "Sistema eléctrico y batería",
                "Revisión de unidad de flota"
            };
            String[] estadosCita = {"ATENDIDA", "CONFIRMADA", "PENDIENTE"};
            int totalCitas = 18;
            for (int c = 1; c <= totalCitas; c++) {
                Vehiculo v = flotaVehiculos.get((c * 4) % flotaVehiculos.size());
                CitaTaller cita = new CitaTaller();
                cita.setFechaHoraCita(fechaEnRango(c - 1, totalCitas, 8 + (c % 9), (c * 15) % 60));
                cita.setCliente(v.getCliente());
                cita.setVehiculo(v);
                cita.setClienteNombre(v.getCliente().getNombreCompleto());
                cita.setClienteEmail(v.getCliente().getEmail());
                cita.setClienteTelefono(v.getCliente().getTelefono());
                cita.setPlacaVehiculo(v.getPlaca());
                cita.setMotivo(motivosCita[c % motivosCita.length]);
                cita.setEstado(estadosCita[c % estadosCita.length]);
                cita.setRegistradoPor(recepcionistas.isEmpty() ? "Camila Torres" : recepcionistas.get(c % recepcionistas.size()).getNombreCompleto());
                citaTallerRepository.save(cita);
            }
        }
    }

    private Rol crearRol(String nombre) {
        Rol rol = new Rol();
        rol.setNombre(nombre);
        return rolRepository.save(rol);
    }

    private Usuario crearUsuario(String nombre, String email, String passwordHash, Rol rol, boolean activo) {
        Usuario user = new Usuario();
        user.setNombreCompleto(nombre);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setActivo(activo);
        user.setRol(rol);
        return usuarioRepository.save(user);
    }

    private Cliente crearCliente(String dniRuc, String nombre, String telefono, String email) {
        Cliente c = new Cliente();
        c.setDniRuc(dniRuc);
        c.setNombreCompleto(nombre);
        c.setTelefono(telefono);
        c.setEmail(email);
        return clienteRepository.save(c);
    }

    private Vehiculo crearVehiculo(String placa, String marca, String modelo, Integer anio, Cliente cliente) {
        Vehiculo v = new Vehiculo();
        v.setPlaca(placa);
        v.setMarca(marca);
        v.setModelo(modelo);
        v.setAnio(anio);
        v.setCliente(cliente);
        return vehiculoRepository.save(v);
    }

    private DetalleRepuesto crearDetalleRepuesto(OrdenTrabajo ot, Repuesto rep, int cantidad) {
        DetalleRepuesto dr = new DetalleRepuesto();
        dr.setOrdenTrabajo(ot);
        dr.setRepuesto(rep);
        dr.setDescripcion(rep.getNombre());
        dr.setCantidad(cantidad);
        dr.setPrecioUnitario(rep.getPrecioUnitario());
        return dr;
    }
}