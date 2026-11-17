package gameshelf.model;

/** Opciones del filtro "Mi calificación". */
public enum EstadoCalificacion {
    TODOS("Todos"),
    SIN_CALIFICAR("Sin calificar"),
    CALIFICADOS("Calificados");

    private final String etiqueta;

    EstadoCalificacion(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }
}
