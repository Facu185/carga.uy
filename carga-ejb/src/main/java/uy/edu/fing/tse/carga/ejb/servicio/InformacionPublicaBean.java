package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.security.PermitAll;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import uy.edu.fing.tse.carga.api.contract.service.InformacionPublicaLocal;
import uy.edu.fing.tse.carga.api.dto.publico.EstadisticasPublicasDTO;
import uy.edu.fing.tse.carga.api.dto.publico.ValidacionEmpresaDTO;
import uy.edu.fing.tse.carga.api.dto.publico.ValidacionVehiculoDTO;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.ejb.modelo.Vehiculo;
import uy.edu.fing.tse.carga.ejb.persistencia.InformacionPublicaDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.InformacionPublicaDAO.Fila;
import uy.edu.fing.tse.carga.ejb.servicio.ProteccionEstadistica.Celda;

import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Frontoffice publico (ADR-0021): validacion de permisos y listados de informacion publica.
 * PermitAll a proposito: es la unica parte del central que se usa sin sesion, y por eso nunca
 * devuelve listados de empresas, vehiculos ni personas.
 */
@Stateless
@PermitAll
public class InformacionPublicaBean implements InformacionPublicaLocal {

    static final ZoneId ZONA = ZoneId.of("America/Montevideo");
    static final int MESES = 12;

    @EJB
    InformacionPublicaDAO dao;

    Clock reloj = Clock.system(ZONA);

    @Override
    public ValidacionVehiculoDTO validarVehiculo(String matricula) throws NegocioException {
        String m = matricula == null ? "" : matricula.toUpperCase(Locale.ROOT).replaceAll("[\\s-]", "");
        if (m.isEmpty() || m.length() > 20 || !m.matches("[A-Z0-9]+")) {
            throw new NegocioException("Ingresá una matrícula válida, por ejemplo STZ1234.");
        }
        LocalDate hoy = LocalDate.now(reloj);
        ValidacionVehiculoDTO r = new ValidacionVehiculoDTO();
        r.setMatricula(m);
        r.setFechaReferencia(hoy.toString());
        dao.vehiculo(m).ifPresent(v -> completar(r, v, hoy));
        return r;
    }

    private static void completar(ValidacionVehiculoDTO r, Vehiculo v, LocalDate hoy) {
        r.setEncontrado(true);
        r.setMatricula(v.getMatricula());
        r.setVehiculoHabilitado(v.isHabilitado());
        r.setEmpresaHabilitada(v.getEmpresa().isActiva());
        r.setEstadoPermiso(estadoPermiso(v, hoy));
        r.setPermisoHasta(texto(v.getPermisoHasta()));
        r.setEstadoItv(v.getItvVencimiento() == null ? "SIN_DATOS"
                : hoy.isAfter(v.getItvVencimiento()) ? "VENCIDO" : "VIGENTE");
        r.setItvVencimiento(texto(v.getItvVencimiento()));
        r.setVigente(r.isVehiculoHabilitado() && r.isEmpresaHabilitada()
                && "VIGENTE".equals(r.getEstadoPermiso()) && "VIGENTE".equals(r.getEstadoItv()));
    }

    /** Mismo criterio que la consulta del funcionario (ConsultaVehiculosBean). */
    static String estadoPermiso(Vehiculo v, LocalDate hoy) {
        if (v.getNumeroPermiso() == null || v.getNumeroPermiso().isBlank()
                || v.getPermisoDesde() == null || v.getPermisoHasta() == null
                || v.getPermisoDesde().isAfter(v.getPermisoHasta())) {
            return "SIN_DATOS";
        }
        if (hoy.isBefore(v.getPermisoDesde())) {
            return "FUTURO";
        }
        return hoy.isAfter(v.getPermisoHasta()) ? "VENCIDO" : "VIGENTE";
    }

    @Override
    public ValidacionEmpresaDTO validarEmpresa(String numeroEmpresa) throws NegocioException {
        long numero;
        try {
            numero = Long.parseLong(numeroEmpresa == null ? "" : numeroEmpresa.trim());
        } catch (NumberFormatException e) {
            throw new NegocioException("El número de empresa tiene que ser un número entero.");
        }
        if (numero <= 0) {
            throw new NegocioException("El número de empresa tiene que ser positivo.");
        }
        ValidacionEmpresaDTO r = new ValidacionEmpresaDTO();
        r.setNumeroEmpresa(numero);
        r.setFechaReferencia(LocalDate.now(reloj).toString());
        dao.empresa(numero).ifPresent(e -> {
            r.setEncontrada(true);
            r.setNombre(e.getNombre());
            r.setHabilitada(e.isActiva());
        });
        return r;
    }

    @Override
    public EstadisticasPublicasDTO estadisticas() {
        YearMonth actual = YearMonth.now(reloj);
        YearMonth primero = actual.minusMonths(MESES - 1);
        List<Fila> guias = dao.guias(primero.atDay(1).atStartOfDay(ZONA).toInstant(),
                actual.plusMonths(1).atDay(1).atStartOfDay(ZONA).toInstant());
        Map<String, Fila> flota = dao.flota(LocalDate.now(reloj)).stream()
                .collect(Collectors.toMap(Fila::grupo, Function.identity()));

        EstadisticasPublicasDTO r = new EstadisticasPublicasDTO();
        r.setDesde(primero.toString());
        r.setHasta(actual.toString());
        r.setUmbralGuias(ProteccionEstadistica.UMBRAL_GUIAS);
        r.setUmbralEmpresas(ProteccionEstadistica.UMBRAL_EMPRESAS);
        r.setGeneradoEn(OffsetDateTime.now(reloj).toString());

        Fila empresas = flota.get("EMPRESAS");
        r.setEmpresasHabilitadas(ProteccionEstadistica.total(empresas.cantidad(), empresas.empresas(),
                ProteccionEstadistica.UMBRAL_EMPRESAS));
        Fila habilitados = flota.get("HABILITADOS");
        r.setVehiculosHabilitados(ProteccionEstadistica.total(habilitados.cantidad(), habilitados.empresas(),
                ProteccionEstadistica.UMBRAL_GUIAS));
        // "Al dia" solo si tambien su complemento cumple: si no, habilitados - al dia lo despeja
        Fila alDia = flota.get("AL_DIA");
        Fila noAlDia = flota.get("NO_AL_DIA");
        boolean complementoSeguro = ProteccionEstadistica.total(noAlDia.cantidad(), noAlDia.empresas(),
                ProteccionEstadistica.UMBRAL_GUIAS) != null;
        r.setVehiculosAlDia(complementoSeguro && r.getVehiculosHabilitados() != null
                ? ProteccionEstadistica.total(alDia.cantidad(), alDia.empresas(), ProteccionEstadistica.UMBRAL_GUIAS)
                : null);

        r.setPorMes(ProteccionEstadistica.serie(meses(guias, primero)));
        r.setPorTipoCarga(ProteccionEstadistica.categorias(celdas(guias, "TIPO"), "Otros tipos de carga"));
        r.setPorRubro(ProteccionEstadistica.categorias(celdas(guias, "RUBRO"), "Otros rubros"));
        return r;
    }

    /** Los 12 meses, incluidos los que no tuvieron guias (en cero). */
    private static List<Celda> meses(List<Fila> filas, YearMonth primero) {
        Map<String, Fila> porMes = filas.stream().filter(f -> "MES".equals(f.grupo()))
                .collect(Collectors.toMap(Fila::clave, Function.identity()));
        List<Celda> resultado = new ArrayList<>();
        for (int i = 0; i < MESES; i++) {
            String mes = primero.plusMonths(i).toString();
            Fila f = porMes.get(mes);
            resultado.add(f == null ? new Celda(mes, mes, 0, Set.of(), 0)
                    : new Celda(mes, mes, f.cantidad(), f.empresas(), f.pesoKg()));
        }
        return resultado;
    }

    private static List<Celda> celdas(List<Fila> filas, String grupo) {
        return filas.stream().filter(f -> grupo.equals(f.grupo()))
                .map(f -> new Celda(f.clave(), f.etiqueta(), f.cantidad(), f.empresas(), f.pesoKg()))
                .toList();
    }

    private static String texto(LocalDate fecha) {
        return fecha == null ? null : fecha.toString();
    }
}
