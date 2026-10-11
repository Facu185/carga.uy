package uy.edu.fing.tse.carga.web.rest;

import jakarta.ws.rs.NameBinding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Marca los endpoints que solo puede llamar un nodo periferico autenticado (ver FiltroTokenNodo). */
@NameBinding
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface NodoAutenticado {
}
