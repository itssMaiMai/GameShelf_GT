package gameshelf.model;

import java.time.LocalDateTime;

/**
 * Datos de un videojuego de la biblioteca COMPARTIDA. Una vez agregado, solo un administrador puede modificarlos.
 * Lo que depende de cada usuario (estado, horas jugadas, favorito, notas y calificación) NO está acá:
 * ver Seguimiento y Calificacion. Convención: anio == 0 significa "sin dato".
 */
public class Videojuego {
    private int id;
    private String nombre = "";
    private String plataforma = "";
    private String genero = "";
    private String desarrollador = "";
    private int anio;
    private LocalDateTime fechaCreacion;
    private String imagen = "";
    private String creadoPor = "";

    public Videojuego() {
    }

    public Videojuego copy() {
        Videojuego c = new Videojuego();
        c.id = id;
        c.nombre = nombre;
        c.plataforma = plataforma;
        c.genero = genero;
        c.desarrollador = desarrollador;
        c.anio = anio;
        c.fechaCreacion = fechaCreacion;
        c.imagen = imagen;
        c.creadoPor = creadoPor;
        return c;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getPlataforma() { return plataforma; }
    public void setPlataforma(String plataforma) { this.plataforma = plataforma; }

    public String getGenero() { return genero; }
    public void setGenero(String genero) { this.genero = genero; }

    public String getDesarrollador() { return desarrollador; }
    public void setDesarrollador(String desarrollador) { this.desarrollador = desarrollador; }

    public int getAnio() { return anio; }
    public void setAnio(int anio) { this.anio = anio; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    /** Nombre del archivo de imagen guardado en la carpeta de imágenes ("" si no tiene imagen). */
    public String getImagen() { return imagen; }
    public void setImagen(String imagen) { this.imagen = imagen == null ? "" : imagen; }

    /** Nombre del usuario que agregó el juego a la biblioteca compartida. */
    public String getCreadoPor() { return creadoPor; }
    public void setCreadoPor(String creadoPor) { this.creadoPor = creadoPor == null ? "" : creadoPor; }
}
