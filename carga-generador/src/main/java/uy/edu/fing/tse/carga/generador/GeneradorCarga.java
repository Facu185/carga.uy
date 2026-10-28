package uy.edu.fing.tse.carga.generador;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public final class GeneradorCarga {
    private static final long TICK_MS = 100;
    private static final String SERIE_NUEVAS = "carga_tracking_posiciones_total{resultado=\"nueva\"}";
    private static final String SERIE_COLA = "carga_tracking_cola_pendientes";

    private final Configuracion config;
    private final ClienteHttp cliente;
    private final Estadisticas estadisticas = new Estadisticas();
    private final List<Vehiculo> vehiculos = new ArrayList<>();
    private final Random azar;
    private final Semaphore enVuelo;
    private PrintWriter csv;
    private ObservadorMonitor monitor;

    private GeneradorCarga(Configuracion config) {
        this.config = config;
        this.cliente = new ClienteHttp(config);
        this.azar = new Random(config.semilla());
        this.enVuelo = new Semaphore(config.concurrencia());
    }

    public static void main(String[] args) throws Exception {
        Configuracion config;
        try {
            config = Configuracion.desdeEntorno(System.getenv());
        } catch (IllegalArgumentException e) {
            System.err.println("[generador] " + e.getMessage());
            System.exit(2);
            return;
        }
        new GeneradorCarga(config).ejecutar();
    }

    private void ejecutar() throws InterruptedException {
        imprimirConfiguracion();
        if (config.token().isEmpty()) {
            System.out.println("[generador] ATENCION: NODO_TOKEN vacio, el central va a rechazar todo con 401");
        }
        abrirCsv();
        iniciarMonitor();
        OptionalDouble nuevasAntes = cliente.leerMetrica(SERIE_NUEVAS);

        long inicio = System.currentTimeMillis();
        long finGeneracion = inicio + config.duracionSeg() * 1000L;
        long finDrenaje = finGeneracion + config.drenajeSeg() * 1000L;
        long proximoReporte = inicio + config.reporteSeg() * 1000L;
        long proximaRampa = inicio + config.rampaCadaSeg() * 1000L;
        long momentoPico = inicio + config.duracionSeg() * 1000L / 3;
        boolean picoHecho = false;
        boolean coberturaRestaurada = false;
        int rotacion = 0;

        agregarVehiculos(config.vehiculos(), inicio);

        while (true) {
            long ahora = System.currentTimeMillis();
            boolean generando = ahora < finGeneracion;

            if (generando && "rampa".equals(config.escenario()) && ahora >= proximaRampa) {
                agregarVehiculos(config.rampaPaso(), ahora);
                proximaRampa += config.rampaCadaSeg() * 1000L;
                System.out.printf("[generador] rampa: ahora %d vehiculos (%.0f posiciones/s esperadas)%n",
                    vehiculos.size(), (double) vehiculos.size() / config.intervaloSeg());
            }
            if (generando && "pico".equals(config.escenario()) && !picoHecho && ahora >= momentoPico) {
                picoHecho = true;
                cortarCoberturaMasiva(ahora);
            }
            if (!generando && !coberturaRestaurada) {
                coberturaRestaurada = true;
                vehiculos.forEach(Vehiculo::restaurarCobertura);
                System.out.println("[generador] fin de la generacion: se envia lo que quedo en los buffers");
            }

            int n = vehiculos.size();
            for (int k = 0; k < n; k++) {
                Vehiculo v = vehiculos.get((rotacion + k) % n);
                if (generando && v.quizasReportar(ahora, config, azar)) {
                    estadisticas.generada();
                }
                if (!enVuelo.tryAcquire()) {
                    continue;
                }
                List<Posicion> envio = v.tomarParaEnviar(ahora, config.loteMax());
                if (envio.isEmpty()) {
                    enVuelo.release();
                    continue;
                }
                enviar(v, envio);
            }
            rotacion = n == 0 ? 0 : (rotacion + 1) % n;

            if (ahora >= proximoReporte) {
                reportar(ahora - inicio);
                proximoReporte += config.reporteSeg() * 1000L;
            }
            if (!generando && (todoEnviado() || ahora >= finDrenaje)) {
                break;
            }
            Thread.sleep(TICK_MS);
        }
        reportar(System.currentTimeMillis() - inicio);
        if (monitor != null) {
            monitor.detener();
        }
        resumen(nuevasAntes);
        if (csv != null) {
            csv.close();
        }
    }

    private void iniciarMonitor() {
        if (!config.monitorActivo()) {
            return;
        }
        ObservadorMonitor observador = new ObservadorMonitor(config);
        try {
            observador.iniciar();
            monitor = observador;
            System.out.println("[generador] funcionario simulado: " + config.monitorUsuario()
                + " consulta el panel de monitoreo cada " + config.monitorCadaSeg() + " s");
        } catch (Exception e) {
            System.out.println("[generador] ATENCION: el funcionario simulado no pudo entrar (" + e.getMessage()
                + "). La prueba sigue solo con los vehiculos.");
        }
    }

    private void enviar(Vehiculo v, List<Posicion> envio) {
        List<Posicion> orden = envio;
        if (envio.size() > 1) {
            orden = new ArrayList<>(envio);
            Collections.shuffle(orden, azar);
        }
        List<Posicion> enviadas = orden;
        cliente.enviar(enviadas).whenComplete((respuesta, error) -> {
            try {
                Respuesta r = respuesta != null ? respuesta : Respuesta.sinRespuesta(0);
                v.resultado(envio, r, System.currentTimeMillis());
                estadisticas.registrar(enviadas, r);
                if (r.tipo() == Respuesta.Tipo.ACEPTADA) {
                    reinyectarDuplicados(v, enviadas, r);
                }
            } finally {
                enVuelo.release();
            }
        });
    }

    private void reinyectarDuplicados(Vehiculo v, List<Posicion> aceptadas, Respuesta r) {
        ThreadLocalRandom azarHilo = ThreadLocalRandom.current();
        for (int i = 0; i < aceptadas.size(); i++) {
            Posicion p = aceptadas.get(i);
            if (!p.duplicada() && !r.rechazadas().contains(i) && azarHilo.nextDouble() < config.probDuplicado()) {
                v.reinyectar(p);
                estadisticas.duplicadaInyectada();
            }
        }
    }

    private void agregarVehiculos(int cantidad, long ahora) {
        int desde = vehiculos.size();
        for (int i = 0; i < cantidad; i++) {
            vehiculos.add(new Vehiculo(desde + i + 1, config.intervaloSeg(), ahora, azar));
        }
    }

    private void cortarCoberturaMasiva(long ahora) {
        int afectados = (int) Math.round(vehiculos.size() * config.picoFraccion());
        List<Vehiculo> elegidos = new ArrayList<>(vehiculos);
        Collections.shuffle(elegidos, azar);
        long vuelta = ahora + config.picoCorteSeg() * 1000L;
        for (Vehiculo v : elegidos.subList(0, afectados)) {
            v.cortarCobertura(vuelta + (long) (azar.nextDouble() * config.picoVentanaSeg() * 1000L));
        }
        double acumuladas = afectados * (double) config.picoCorteSeg() / config.intervaloSeg();
        System.out.printf("[generador] pico: %d vehiculos sin cobertura por %d s; al volver descargan unas %.0f"
            + " posiciones en %d s%n", afectados, config.picoCorteSeg(), acumuladas, config.picoVentanaSeg());
    }

    private boolean todoEnviado() {
        for (Vehiculo v : vehiculos) {
            if (v.pendientes() > 0 || v.enVuelo()) {
                return false;
            }
        }
        return true;
    }

    private int pendientesEnBuffers() {
        int total = 0;
        for (Vehiculo v : vehiculos) {
            total += v.pendientes();
        }
        return total;
    }

    private void reportar(long transcurridoMs) {
        Estadisticas.Intervalo i = estadisticas.tomarIntervalo(config.reporteSeg());
        ObservadorMonitor.Intervalo m = monitor != null ? monitor.tomarIntervalo() : null;
        int enBuffer = pendientesEnBuffers();
        long segundo = transcurridoMs / 1000;
        String lineaMonitor = textoMonitor(m);
        System.out.printf(Locale.ROOT, "[generador] t=%4ds veh=%5d | %6.1f ped/s %7.1f pos/s | 202=%6d 400=%3d"
                + " 503=%6d err=%4d | p50=%4dms p95=%5dms p99=%5dms | en buffer=%d%s%n",
            segundo, vehiculos.size(), i.pedidosPorSeg(), i.posicionesPorSeg(), i.aceptadas(), i.rechazadas(),
            i.saturadas(), i.errores(), i.p50(), i.p95(), i.p99(), enBuffer, lineaMonitor);
        if (csv != null) {
            String columnasMonitor = m == null || m.consultas() == 0 ? ","
                : m.metricasP95() + "," + m.mapaP95();
            csv.printf(Locale.ROOT, "%d,%d,%.2f,%.2f,%d,%d,%d,%d,%d,%d,%d,%d,%s%n", segundo, vehiculos.size(),
                i.pedidosPorSeg(), i.posicionesPorSeg(), i.aceptadas(), i.rechazadas(), i.saturadas(),
                i.errores(), i.p50(), i.p95(), i.p99(), enBuffer, columnasMonitor);
            csv.flush();
        }
    }

    private static String textoMonitor(ObservadorMonitor.Intervalo m) {
        if (m == null) {
            return "";
        }
        String errores = m.errores() > 0 ? " (" + m.errores() + " errores)" : "";
        if (m.consultas() == 0) {
            return " | monitor: sin respuestas" + errores;
        }
        return String.format(Locale.ROOT, " | monitor: metricas p95=%dms mapa p95=%dms%s",
            m.metricasP95(), m.mapaP95(), errores);
    }

    private void resumen(OptionalDouble nuevasAntes) throws InterruptedException {
        Estadisticas.Total t = estadisticas.total();
        System.out.println();
        System.out.println("================ RESUMEN ================");
        System.out.printf("Escenario: %s | vehiculos al final: %d%n", config.escenario(), vehiculos.size());
        System.out.printf("Posiciones generadas:            %d%n", t.generadas());
        System.out.printf("Reenvios intencionales:          %d%n", t.duplicadasInyectadas());
        System.out.printf("Pedidos HTTP:                    %d%n", t.pedidos());
        System.out.printf("Posiciones aceptadas (202):      %d (unicas: %d)%n", t.aceptadas(), t.unicasAceptadas());
        System.out.printf("Rechazadas (400):                %d%n", t.rechazadas());
        System.out.printf("Rechazos por saturacion (503):   %d (se reintentaron)%n", t.saturadas());
        System.out.printf("Pedidos con error de red/5xx:    %d (se reintentaron)%n", t.errores());
        System.out.printf("Sin enviar al terminar:          %d%n", pendientesEnBuffers());
        System.out.printf("Latencia de la ingesta: p50=%dms p95=%dms p99=%dms max=%dms%n",
            t.p50(), t.p95(), t.p99(), t.maximo());
        if (monitor != null) {
            ObservadorMonitor.Total m = monitor.total();
            System.out.printf("Panel de monitoreo (%d consultas): metricas p50=%dms p95=%dms max=%dms |"
                    + " mapa p50=%dms p95=%dms max=%dms | errores=%d%n", m.consultas(), m.metricasP50(),
                m.metricasP95(), m.metricasMax(), m.mapaP50(), m.mapaP95(), m.mapaMax(), m.errores());
        }
        verificarSinPerdida(nuevasAntes, t);
        if (csv != null) {
            System.out.println("Detalle por intervalo: " + config.archivoCsv());
        }
    }

    private void verificarSinPerdida(OptionalDouble nuevasAntes, Estadisticas.Total t) throws InterruptedException {
        if (nuevasAntes.isEmpty()) {
            System.out.println("Verificacion sin perdida: no se pudieron leer las metricas del central al inicio.");
            return;
        }
        System.out.println("Esperando a que el central termine de procesar la cola...");
        for (int i = 0; i < 60; i++) {
            OptionalDouble cola = cliente.leerMetrica(SERIE_COLA);
            if (cola.isPresent() && cola.getAsDouble() == 0) {
                break;
            }
            Thread.sleep(2000);
        }
        Thread.sleep(2000);
        OptionalDouble nuevasDespues = cliente.leerMetrica(SERIE_NUEVAS);
        if (nuevasDespues.isEmpty()) {
            System.out.println("Verificacion sin perdida: no se pudieron leer las metricas del central al final.");
            return;
        }
        long guardadas = Math.round(nuevasDespues.getAsDouble() - nuevasAntes.getAsDouble());
        System.out.printf("Guardadas como nuevas en el central: %d | unicas aceptadas: %d%n",
            guardadas, t.unicasAceptadas());
        if (guardadas == t.unicasAceptadas()) {
            System.out.println("RESULTADO: SIN PERDIDA. Todo lo aceptado quedo guardado una sola vez.");
        } else if (guardadas > t.unicasAceptadas()) {
            System.out.println("RESULTADO: el central guardo MAS de lo esperado. Hay otro emisor activo"
                + " (el nodo Tracking de demostracion?) o el central se reinicio durante la prueba.");
        } else {
            System.out.println("RESULTADO: FALTAN " + (t.unicasAceptadas() - guardadas) + " posiciones. Revisar"
                + " la DLQ y los logs del central.");
        }
    }

    private void abrirCsv() {
        try {
            Path archivo = Path.of(config.archivoCsv());
            if (archivo.getParent() != null) {
                Files.createDirectories(archivo.getParent());
            }
            csv = new PrintWriter(Files.newBufferedWriter(archivo, StandardCharsets.UTF_8));
            csv.println("segundo,vehiculos,pedidos_s,posiciones_s,aceptadas,rechazadas,saturadas,errores,"
                + "p50_ms,p95_ms,p99_ms,en_buffer,monitor_metricas_p95_ms,monitor_mapa_p95_ms");
        } catch (IOException e) {
            System.out.println("[generador] no se pudo crear el CSV (" + e.getMessage() + "): solo consola");
        }
    }

    private void imprimirConfiguracion() {
        System.out.println("[generador] escenario=" + config.escenario()
            + " vehiculos=" + config.vehiculos()
            + " intervalo=" + config.intervaloSeg() + "s"
            + " duracion=" + config.duracionSeg() + "s"
            + " -> " + String.format(Locale.ROOT, "%.1f", config.eventosPorSegundoBase()) + " posiciones/s base");
        System.out.println("[generador] destino=" + config.urlIngesta() + " concurrencia=" + config.concurrencia()
            + " duplicados=" + config.probDuplicado() + " sinCobertura=" + config.probSinCobertura());
    }
}
