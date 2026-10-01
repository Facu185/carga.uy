package uy.edu.fing.tse.carga.ejb.integracion;

import jakarta.xml.ws.handler.MessageContext;
import jakarta.xml.ws.handler.soap.SOAPMessageContext;
import org.junit.jupiter.api.Test;
import uy.edu.fing.tse.carga.api.observabilidad.Correlacion;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class EncabezadoCorrelacionHandlerTest {

    /** Contexto SOAP falso respaldado por un mapa (SOAPMessageContext es un Map). */
    private static SOAPMessageContext contexto(Map<String, Object> datos) {
        return (SOAPMessageContext) Proxy.newProxyInstance(getClassLoader(), new Class<?>[]{SOAPMessageContext.class},
                (proxy, metodo, args) -> metodo.invoke(datos, args));
    }

    private static ClassLoader getClassLoader() {
        return EncabezadoCorrelacionHandlerTest.class.getClassLoader();
    }

    @Test
    @SuppressWarnings("unchecked")
    void agregaElHeaderEnLasLlamadasSalientes() throws Exception {
        Map<String, Object> datos = new HashMap<>(Map.of(MessageContext.MESSAGE_OUTBOUND_PROPERTY, true));

        Correlacion.ejecutarCon("id-12345678", () -> new EncabezadoCorrelacionHandler().handleMessage(contexto(datos)));

        Map<String, List<String>> headers = (Map<String, List<String>>) datos.get(MessageContext.HTTP_REQUEST_HEADERS);
        assertEquals(List.of("id-12345678"), headers.get(Correlacion.HEADER));
    }

    @Test
    void noTocaLasRespuestasNiLasLlamadasSinId() throws Exception {
        Map<String, Object> entrante = new HashMap<>(Map.of(MessageContext.MESSAGE_OUTBOUND_PROPERTY, false));
        Correlacion.ejecutarCon("id-12345678", () -> new EncabezadoCorrelacionHandler().handleMessage(contexto(entrante)));
        assertNull(entrante.get(MessageContext.HTTP_REQUEST_HEADERS));

        Map<String, Object> sinId = new HashMap<>(Map.of(MessageContext.MESSAGE_OUTBOUND_PROPERTY, true));
        new EncabezadoCorrelacionHandler().handleMessage(contexto(sinId));
        assertNull(sinId.get(MessageContext.HTTP_REQUEST_HEADERS));
    }
}
