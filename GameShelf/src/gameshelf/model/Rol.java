package gameshelf.model;

/** Rol de un usuario. Solo el administrador puede dar de baja juegos. */
public enum Rol {
    ADMIN("Administrador"),
    USUARIO("Usuario");

    private final String etiqueta;

    Rol(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    @Override
    public String toString() {
        return etiqueta;
    }

    public static Rol desdeTexto(String texto) {
        if (texto != null && texto.trim().equalsIgnoreCase("ADMIN")) {
            return ADMIN;
        }
        return USUARIO;
    }
}
