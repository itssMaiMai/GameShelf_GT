package gameshelf.service;

/** Error de regla de negocio / validación; su mensaje se puede mostrar tal cual al usuario. */
public class ValidacionException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ValidacionException(String mensaje) {
        super(mensaje);
    }
}
