package uy.edu.fing.tse.carga.api.dto.comun;

import jakarta.xml.bind.annotation.adapters.XmlAdapter;

import java.time.LocalDate;

/** JAXB no mapea java.time por defecto: serializa LocalDate como texto ISO en SOAP. */
public class FechaAdapter extends XmlAdapter<String, LocalDate> {

    @Override
    public LocalDate unmarshal(String v) {
        return (v == null || v.isBlank()) ? null : LocalDate.parse(v);
    }

    @Override
    public String marshal(LocalDate v) {
        return v == null ? null : v.toString();
    }
}
