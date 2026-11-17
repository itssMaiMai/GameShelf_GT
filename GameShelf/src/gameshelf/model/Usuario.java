package gameshelf.model;

/** Cuenta de usuario. La contraseña nunca se guarda: solo su hash (PBKDF2) y la sal. */
public class Usuario {
    private int id;
    private String nombre = "";
    private Rol rol = Rol.USUARIO;
    private String salt = "";
    private String hash = "";

    public Usuario copy() {
        Usuario u = new Usuario();
        u.id = id;
        u.nombre = nombre;
        u.rol = rol;
        u.salt = salt;
        u.hash = hash;
        return u;
    }

    public boolean isAdmin() { return rol == Rol.ADMIN; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Rol getRol() { return rol; }
    public void setRol(Rol rol) { this.rol = rol; }

    public String getSalt() { return salt; }
    public void setSalt(String salt) { this.salt = salt; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}
