package uy.edu.fing.tse.carga.ejb.observabilidad;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.ConcurrencyManagement;
import jakarta.ejb.ConcurrencyManagementType;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import uy.edu.fing.tse.carga.api.contract.service.MetricasLocal;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MonitorRecursos {
    static final String POOL = "jboss.as:subsystem=datasources,data-source=CargaDS,statistics=pool";

    /**
     * Nombre exacto de cada atributo, resuelto una sola vez. getMBeanInfo sobre un MBean de WildFly
     * reconstruye su modelo de administracion entero: hacerlo en cada lectura costaba ~7 % de la CPU
     * del central durante la prueba de performance (perfil JFR, docs/informe-performance.md).
     */
    private static final Map<String, String> NOMBRES = new ConcurrentHashMap<>();

    @EJB
    MetricasLocal metricas;

    @PostConstruct
    void registrar() {
        metricas.registrarMedidor("carga_jvm_memoria_heap_maxima_bytes",
            () -> ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getMax());
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            String nombre = gc.getName().toLowerCase(Locale.ROOT).replace(' ', '_');
            metricas.registrarMedidor("carga_jvm_gc_colecciones_total", gc::getCollectionCount, "recolector", nombre);
            metricas.registrarMedidor("carga_jvm_gc_tiempo_ms_total", gc::getCollectionTime, "recolector", nombre);
        }
        metricas.registrarMedidor("carga_jvm_cpu_proceso",
            () -> atributo("java.lang:type=OperatingSystem", "ProcessCpuLoad"));
        metricas.registrarMedidor("carga_db_conexiones", () -> atributo(POOL, "InUseCount"), "estado", "en_uso");
        metricas.registrarMedidor("carga_db_conexiones", () -> atributo(POOL, "AvailableCount"),
            "estado", "disponibles");
        metricas.registrarMedidor("carga_db_conexiones_max_usadas", () -> atributo(POOL, "MaxUsedCount"));
        metricas.registrarMedidor("carga_db_esperas_total", () -> atributo(POOL, "WaitCount"));
        metricas.registrarMedidor("carga_db_espera_promedio_ms", () -> atributo(POOL, "AverageBlockingTime"));
    }

    static double atributo(String objeto, String atributo) {
        try {
            MBeanServer servidor = ManagementFactory.getPlatformMBeanServer();
            ObjectName nombre = new ObjectName(objeto);
            if (!servidor.isRegistered(nombre)) {
                return -1;
            }
            String clave = objeto + '#' + atributo;
            String exacto = NOMBRES.get(clave);
            if (exacto == null) {
                for (MBeanAttributeInfo info : servidor.getMBeanInfo(nombre).getAttributes()) {
                    if (info.getName().equalsIgnoreCase(atributo)) {
                        exacto = info.getName();
                        NOMBRES.put(clave, exacto);
                    }
                }
            }
            return exacto == null ? -1 : numero(servidor.getAttribute(nombre, exacto));
        } catch (Exception e) {
            return -1;
        }
    }

    static double numero(Object valor) {
        if (valor instanceof Number n) {
            return n.doubleValue();
        }
        if (valor != null) {
            try {
                return Double.parseDouble(valor.toString().trim());
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }
}
