package gameshelf.service;

import gameshelf.model.Rol;
import gameshelf.model.Usuario;
import gameshelf.persistence.UsuarioRepository;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** Registro e inicio de sesión. Las contraseñas se guardan con PBKDF2-HMAC-SHA256 y una sal aleatoria. */
public class UsuarioService {
    private static final int ITERACIONES = 120_000;
    private static final int MIN_CLAVE = 6;
    private static final int MAX_CLAVE = 100;

    private final UsuarioRepository repository;
    private final SecureRandom azar = new SecureRandom();

    public UsuarioService(UsuarioRepository repository) {
        this.repository = repository;
    }

    public boolean hayUsuarios() {
        return repository.cantidad() > 0;
    }

    /** Crea la primera cuenta, que es administradora. Solo se permite si todavía no existe ningún usuario. */
    public synchronized Usuario crearAdministradorInicial(String nombre, char[] clave, char[] confirmacion) {
        if (repository.cantidad() > 0) {
            throw new ValidacionException("Ya existe un administrador. Iniciá sesión con tu cuenta.");
        }
        return crear(nombre, clave, confirmacion, Rol.ADMIN);
    }

    /** Registro abierto: crea una cuenta con rol Usuario (no puede dar de baja juegos). */
    public synchronized Usuario registrar(String nombre, char[] clave, char[] confirmacion) {
        if (repository.cantidad() == 0) {
            throw new ValidacionException("Primero hay que crear la cuenta de administrador.");
        }
        return crear(nombre, clave, confirmacion, Rol.USUARIO);
    }

    public Usuario iniciarSesion(String nombre, char[] clave) {
        Usuario u = nombre == null || nombre.trim().isEmpty() ? null : repository.buscarPorNombre(nombre);
        if (u == null || clave == null || clave.length == 0) {
            throw new ValidacionException("Usuario o contraseña incorrectos.");
        }
        byte[] esperado = Base64.getDecoder().decode(u.getHash());
        byte[] calculado = hash(clave, Base64.getDecoder().decode(u.getSalt()));
        if (!MessageDigest.isEqual(esperado, calculado)) {
            throw new ValidacionException("Usuario o contraseña incorrectos.");
        }
        return u;
    }

    private Usuario crear(String nombre, char[] clave, char[] confirmacion, Rol rol) {
        String n = nombre == null ? "" : nombre.trim();
        if (n.length() < 3 || n.length() > 30) {
            throw new ValidacionException("El nombre de usuario debe tener entre 3 y 30 caracteres.");
        }
        if (!n.matches("[\\p{L}\\p{N}_.\\-]+")) {
            throw new ValidacionException("El nombre de usuario solo puede tener letras, números, punto, guion y guion bajo.");
        }
        if (clave == null || clave.length < MIN_CLAVE || clave.length > MAX_CLAVE) {
            throw new ValidacionException("La contraseña debe tener entre " + MIN_CLAVE + " y " + MAX_CLAVE + " caracteres.");
        }
        if (confirmacion == null || !Arrays.equals(clave, confirmacion)) {
            throw new ValidacionException("Las contraseñas no coinciden.");
        }
        if (repository.buscarPorNombre(n) != null) {
            throw new ValidacionException("Ya existe un usuario llamado \"" + n + "\".");
        }
        byte[] salt = new byte[16];
        azar.nextBytes(salt);
        Usuario u = new Usuario();
        u.setNombre(n);
        u.setRol(rol);
        u.setSalt(Base64.getEncoder().encodeToString(salt));
        u.setHash(Base64.getEncoder().encodeToString(hash(clave, salt)));
        repository.guardar(u);
        return u;
    }

    private static byte[] hash(char[] clave, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(clave, salt, ITERACIONES, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash de la contraseña.", e);
        } finally {
            spec.clearPassword();
        }
    }
}
