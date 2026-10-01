package uy.edu.fing.tse.carga.ejb.integracion;

import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPHandler;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import uy.edu.fing.tse.carga.api.observabilidad.Correlacion;

import javax.xml.namespace.QName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Agrega el ID de correlacion como header HTTP en cada llamada SOAP que sale hacia la PDI. */
public class EncabezadoCorrelacionHandler implements SOAPHandler<SOAPMessageContext> {

    @Override
    @SuppressWarnings("unchecked")
    public boolean handleMessage(SOAPMessageContext contexto) {
        boolean saliente = Boolean.TRUE.equals(contexto.get(MessageContext.MESSAGE_OUTBOUND_PROPERTY));
        if (saliente) {
            Correlacion.actual().ifPresent(id -> {
                Map<String, List<String>> headers =
                        (Map<String, List<String>>) contexto.get(MessageContext.HTTP_REQUEST_HEADERS);
                Map<String, List<String>> nuevos = headers == null ? new HashMap<>() : new HashMap<>(headers);
                nuevos.put(Correlacion.HEADER, List.of(id));
                contexto.put(MessageContext.HTTP_REQUEST_HEADERS, nuevos);
            });
        }
        return true;
    }

    @Override
    public boolean handleFault(SOAPMessageContext contexto) {
        return true;
    }

    @Override
    public void close(MessageContext contexto) {
        // nada que liberar
    }

    @Override
    public Set<QName> getHeaders() {
        return Set.of();
    }
}
