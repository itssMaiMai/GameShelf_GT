package gameshelf.model;

/** Estados posibles de un videojuego dentro de la biblioteca. */
public enum Estado {
    PENDIENTE("Pendiente"),
    JUGANDO("Jugando"),
    TERMINADO("Terminado"),
    ABANDONADO("Abandonado");

    private final String etiqueta;

    Estado(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }

    /** Convierte un texto (nombre del enum o etiqueta) en un Estado; si no reconoce el texto devuelve PENDIENTE. */
    public static Estado desdeTexto(String texto) {
        if (texto != null) {
            for (Estado e : values()) {
                if (e.name().equalsIgnoreCase(texto.trim()) || e.etiqueta.equalsIgnoreCase(texto.trim())) {
                    return e;
                }
            }
        }
        return PENDIENTE;
    }
}
