package uy.edu.fing.tse.carga.ejb.servicio;

import jakarta.annotation.Resource;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ejb.EJB;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import uy.edu.fing.tse.carga.api.contract.annotation.AccionAuditada;
import uy.edu.fing.tse.carga.api.contract.service.AuditoriaLocal;
import uy.edu.fing.tse.carga.api.contract.service.InfraccionesEmpresaLocal;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.CasoDTO;
import uy.edu.fing.tse.carga.api.dto.fiscalizacion.PaginaCasosDTO;
import uy.edu.fing.tse.carga.api.exception.ConflictoException;
import uy.edu.fing.tse.carga.api.exception.NegocioException;
import uy.edu.fing.tse.carga.api.exception.RecursoAjenoException;
import uy.edu.fing.tse.carga.api.security.Roles;
import uy.edu.fing.tse.carga.ejb.modelo.CasoFiscalizacion;
import uy.edu.fing.tse.carga.ejb.modelo.Descargo;
import uy.edu.fing.tse.carga.ejb.modelo.EstadoCaso;
import uy.edu.fing.tse.carga.ejb.persistencia.FiscalizacionDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.UsuarioDAO;
import uy.edu.fing.tse.carga.ejb.persistencia.VinculoEmpresaUsuarioDAO;
import uy.edu.fing.tse.carga.ejb.seguridad.CompatibilidadFuncionarioEmpresa;

import java.time.Instant;
import java.util.List;

/**
 * Infracciones de una empresa para su responsable, con la evidencia y el descargo (letra, frontoffice de
 * la empresa, punto 5; ADR-0022). La empresa se autoriza en el servidor en cada pedido: un id de empresa
 * ajeno o un caso de otra empresa responden 403, igual que si no existiera.
 */
@Stateless
@RolesAllowed({Roles.CIUDADANO, Roles.CHOFER})
public class InfraccionesEmpresaBean implements InfraccionesEmpresaLocal {

    @EJB
    FiscalizacionDAO dao;

    @EJB
    VinculoEmpresaUsuarioDAO vinculos;

    @EJB
    UsuarioDAO usuarios;

    @EJB
    AuditoriaLocal auditoria;

    @Inject
    CompatibilidadFuncionarioEmpresa compatibilidad;

    @Resource
    SessionContext contexto;

    @Override
    public PaginaCasosDTO infracciones(Long empresaId, Integer pagina, Integer tamano) throws NegocioException {
        autorizar(empresaId);
        int p = Casos.pagina(pagina);
        int n = Casos.tamano(tamano);
        List<CasoFiscalizacion> casos = dao.buscar(empresaId, null, null, p * n, n);
        return Casos.pagina(casos, dao.descargos(casos.stream().map(CasoFiscalizacion::getId).toList()),
                dao.contar(empresaId, null, null), p, n);
    }

    @Override
    public CasoDTO infraccion(Long empresaId, Long casoId) throws NegocioException {
        CasoFiscalizacion c = deLaEmpresa(empresaId, casoId);
        return Casos.aDTO(c, dao.descargo(casoId).orElse(null), Instant.now());
    }

    @Override
    public CasoDTO presentarDescargo(Long empresaId, Long casoId, String fundamento) throws NegocioException {
        String f = Casos.fundamento(fundamento, "descargo");
        CasoFiscalizacion c = deLaEmpresa(empresaId, casoId);
        Instant ahora = Instant.now();
        if (dao.descargo(casoId).isPresent()) {
            throw new ConflictoException("La empresa ya presentó su descargo para este caso.");
        }
        if (c.getEstado() != EstadoCaso.NOTIFICADA) {
            throw new ConflictoException("El caso está " + c.getEstado() + ": ya no admite descargo.");
        }
        if (c.plazoVencido(ahora)) {
            throw new ConflictoException("El plazo para presentar el descargo venció el " + c.getVenceDescargo() + ".");
        }
        String cedula = cedula();
        dao.guardar(new Descargo(casoId, usuarios.buscarPorCedula(cedula).orElse(null), f, ahora));
        c.pasarA(EstadoCaso.EN_DESCARGO);
        auditoria.registrar(AccionAuditada.CASO_CAMBIO_ESTADO, "CasoFiscalizacion", casoId, f,
                "NOTIFICADA -> EN_DESCARGO: descargo de la empresa " + empresaId);
        Descargo descargo = dao.descargo(casoId).orElse(null);
        return Casos.aDTO(c, descargo, ahora);
    }

    private CasoFiscalizacion deLaEmpresa(Long empresaId, Long casoId) throws NegocioException {
        autorizar(empresaId);
        if (casoId == null || casoId <= 0) throw new NegocioException("El caso indicado no es válido.");
        CasoFiscalizacion c = dao.caso(casoId).orElseThrow(() -> new RecursoAjenoException("El caso no es de esta empresa."));
        if (c.getEmpresa() == null || !c.getEmpresa().getId().equals(empresaId)) {
            throw new RecursoAjenoException("El caso no es de esta empresa.");
        }
        return c;
    }

    private void autorizar(Long empresaId) throws NegocioException {
        if (empresaId == null || empresaId <= 0) throw new NegocioException("La empresa indicada no es válida.");
        String cedula = cedula();
        compatibilidad.verificarEjercicioResponsabilidad(cedula);
        if (!vinculos.esResponsableActivo(cedula, empresaId)) {
            throw new RecursoAjenoException("No tiene un vínculo responsable activo con esa empresa.");
        }
    }

    private String cedula() throws NegocioException {
        if (contexto == null || contexto.getCallerPrincipal() == null
                || contexto.getCallerPrincipal().getName() == null || contexto.getCallerPrincipal().getName().isBlank()) {
            throw new NegocioException("No hay un usuario autenticado.");
        }
        return contexto.getCallerPrincipal().getName();
    }
}
