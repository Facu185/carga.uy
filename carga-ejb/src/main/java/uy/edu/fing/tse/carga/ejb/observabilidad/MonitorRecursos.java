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


@Singleton
@Startup
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class MonitorRecursos {
    static final String POOL = "jboss.as:subsystem=datasources,data-source=CargaDS,statistics=pool";

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
            for (MBeanAttributeInfo info : servidor.getMBeanInfo(nombre).getAttributes()) {
                if (info.getName().equalsIgnoreCase(atributo)) {
                    return numero(servidor.getAttribute(nombre, info.getName()));
                }
            }
            return -1;
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
