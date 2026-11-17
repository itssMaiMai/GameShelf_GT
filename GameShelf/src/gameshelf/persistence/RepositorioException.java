package gameshelf.persistence;

/** Error al leer o escribir el archivo de datos. */
public class RepositorioException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RepositorioException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
