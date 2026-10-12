package uy.edu.fing.tse.carga.api.contract.annotation;

/**
 * Acciones que quedan en la pista de auditoria. Las de fiscalizacion exigen fundamento.
 * Cada frente agrega las suyas (agregar es compatible; renombrar o quitar no).
 */
public enum AccionAuditada {
    // Fiscalizacion (P5)
    CASO_CREADO(true),
    CASO_CAMBIO_ESTADO(true),
    CASO_RESUELTO(true),
    // Administracion (P1)
    USUARIO_INTERNO_CREADO(false),
    USUARIO_INTERNO_ESTADO_CAMBIADO(false),
    USUARIO_INTERNO_ROL_CAMBIADO(false),
    USUARIO_INTERNO_DESBLOQUEADO(false),
    FUNCIONARIO_HABILITADO(false),
    FUNCIONARIO_ESTADO_CAMBIADO(false),
    FUNCIONARIO_DEPENDENCIA_CAMBIADA(false),
    SOLICITUD_FUNCIONARIO_CREADA(false),
    SOLICITUD_FUNCIONARIO_APROBADA(false),
    SOLICITUD_FUNCIONARIO_RECHAZADA(true),
    // Empresas y autorizaciones empresariales
    SOLICITUD_EMPRESA_CREADA(false),
    SOLICITUD_EMPRESA_APROBADA(false),
    SOLICITUD_EMPRESA_RECHAZADA(true),
    VINCULO_EMPRESA_SOLICITADO(false),
    VINCULO_EMPRESA_APROBADO(false),
    VINCULO_EMPRESA_TRANSFERIDO(true),
    VINCULO_EMPRESA_RECHAZADO(true),
    EMPRESA_DADA_DE_BAJA(true),
    // Seguridad de acceso
    INGRESO_EXITOSO(false),
    INGRESO_FALLIDO(false),
    CUENTA_BLOQUEADA(false),
    PASSWORD_RESETEADA(false),
    PASSWORD_CAMBIADA(false);

    private final boolean requiereFundamento;

    AccionAuditada(boolean requiereFundamento) {
        this.requiereFundamento = requiereFundamento;
    }

    public boolean requiereFundamento() {
        return requiereFundamento;
    }
}
