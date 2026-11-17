package gameshelf;

import org.h2.tools.Server;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

/**
 * Modo servidor: {@code java -jar GameShelf.jar --servidor [--puerto 9092] [--carpeta ruta]}.
 * Publica la base de datos de la carpeta indicada (por defecto, la actual) para que otras PCs de la red se conecten
 * con sus propios GameShelf. La base (gameshelf-db.mv.db) se crea si no existe.
 */
public final class Servidor {
    private Servidor() {
    }

    public static void ejecutar(String[] args) {
        int puerto = 9092;
        Path carpeta = Paths.get("").toAbsolutePath();
        for (int i = 1; i < args.length; i++) {
            if ("--puerto".equals(args[i]) && i + 1 < args.length) {
                puerto = Integer.parseInt(args[++i]);
            } else if ("--carpeta".equals(args[i]) && i + 1 < args.length) {
                carpeta = Paths.get(args[++i]).toAbsolutePath();
            }
        }
        try {
            Server servidor = Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", String.valueOf(puerto),
                    "-baseDir", carpeta.toString(), "-ifNotExists").start();
            Runtime.getRuntime().addShutdownHook(new Thread(servidor::stop));

            System.out.println("Servidor GameShelf iniciado.");
            System.out.println("Carpeta de la base de datos: " + carpeta + "  (archivo: gameshelf-db.mv.db)");
            System.out.println("Puerto: " + puerto + "  (si hay firewall, habilit\u00e1 ese puerto de entrada)");
            System.out.println();
            System.out.println("En las otras PCs: GameShelf > Iniciar sesi\u00f3n > Conexi\u00f3n... > Servidor, y usar una de estas direcciones:");
            for (String ip : direcciones()) {
                System.out.println("   " + ip + "   (puerto " + puerto + ", base \"gameshelf-db\")");
            }
            System.out.println("Usuario y contrase\u00f1a de la base: " + gameshelf.persistence.ConexionDB.USUARIO_POR_DEFECTO + " / "
                    + gameshelf.persistence.ConexionDB.CLAVE_POR_DEFECTO + " (los del programa por defecto)");
            System.out.println();
            System.out.println("Dej\u00e1 esta ventana abierta. Para detener el servidor: Ctrl+C.");
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            System.err.println("No se pudo iniciar el servidor: " + e.getMessage());
            System.exit(1);
        }
    }

    private static java.util.List<String> direcciones() {
        java.util.List<String> ips = new java.util.ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address) {
                        ips.add(a.getHostAddress());
                    }
                }
            }
        } catch (Exception e) {
            // se informa solo localhost
        }
        if (ips.isEmpty()) {
            ips.add("localhost");
        }
        return ips;
    }
}
