package uy.edu.fing.tse.carga.ejb.modelo;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import uy.edu.fing.tse.carga.api.exception.NegocioException;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.OffsetDateTime;

/** Nodo periferico registrado (tabla de la V1; reglas de la V9). */
@Entity
@Table(name = "nodo_periferico")
public class NodoPeriferico {

    static final int LARGO_MAXIMO_NOMBRE = 100;
    static final int LARGO_MAXIMO_URL = 300;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = LARGO_MAXIMO_NOMBRE)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoNodo tipo;

    @Column(length = LARGO_MAXIMO_URL)
    private String url;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "actualizado_en", nullable = false)
    private OffsetDateTime actualizadoEn;

    protected NodoPeriferico() {
    }

    /** Un nodo nuevo nace desactivado: activarlo es una decision aparte, que queda auditada. */
    public static NodoPeriferico nuevoBalanzas(String nombre, String url) throws NegocioException {
        NodoPeriferico n = new NodoPeriferico();
        n.tipo = TipoNodo.BALANZAS;
        n.nombre = nombreValido(nombre);
        n.url = urlValida(url);
        n.activo = false;
        n.actualizadoEn = OffsetDateTime.now();
        return n;
    }

    public void renombrar(String nuevo) throws NegocioException {
        nombre = nombreValido(nuevo);
        actualizadoEn = OffsetDateTime.now();
    }

    public void cambiarUrl(String nueva) throws NegocioException {
        if (tipo != TipoNodo.BALANZAS) {
            throw new NegocioException("Solo los nodos Balanzas tienen URL: el nodo Tracking es el que llama al central.");
        }
        url = urlValida(nueva);
        actualizadoEn = OffsetDateTime.now();
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
        actualizadoEn = OffsetDateTime.now();
    }

    static String nombreValido(String nombre) throws NegocioException {
        if (nombre == null || nombre.isBlank()) {
            throw new NegocioException("El nombre del nodo es obligatorio.");
        }
        String limpio = nombre.trim();
        if (limpio.length() > LARGO_MAXIMO_NOMBRE) {
            throw new NegocioException("El nombre puede tener hasta " + LARGO_MAXIMO_NOMBRE + " caracteres.");
        }
        return limpio;
    }

    /** Una URL que el central pueda consultar: http o https, con servidor. */
    static String urlValida(String url) throws NegocioException {
        if (url == null || url.isBlank()) {
            throw new NegocioException("La URL del nodo es obligatoria.");
        }
        String limpia = url.trim();
        if (limpia.length() > LARGO_MAXIMO_URL) {
            throw new NegocioException("La URL puede tener hasta " + LARGO_MAXIMO_URL + " caracteres.");
        }
        try {
            URI uri = new URI(limpia);
            String esquema = uri.getScheme();
            if (esquema == null || !(esquema.equalsIgnoreCase("http") || esquema.equalsIgnoreCase("https"))
                    || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new NegocioException("La URL tiene que empezar con http:// o https:// e indicar el servidor.");
            }
        } catch (URISyntaxException e) {
            throw new NegocioException("La URL no es valida.");
        }
        return limpia;
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public TipoNodo getTipo() { return tipo; }
    public String getUrl() { return url; }
    public boolean isActivo() { return activo; }
    public OffsetDateTime getActualizadoEn() { return actualizadoEn; }
}
