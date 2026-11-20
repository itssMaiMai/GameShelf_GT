# GameShelf – Administrador de biblioteca de videojuegos

Aplicación de escritorio (Java + Swing + base de datos H2) para organizar una colección de videojuegos compartida entre usuarios.
No es un launcher: no abre, instala ni descarga juegos, ni se conecta a ninguna plataforma.

## Cómo ejecutarla (requiere Java 17 o superior)
- Terminal: `java -jar GameShelf.jar` (en Linux, el doble clic puede estar bloqueado por seguridad).
- `ejecutar.bat` (Windows) / `./ejecutar.sh` (Linux/macOS). Si falta `GameShelf.jar`, lo arman con `construir.bat` / `construir.sh`
  (hace falta un JDK 17+; el controlador de base de datos H2 está en `lib/h2.jar`).
- En un IDE: abrir `src`, agregar `lib/h2.jar` a las librerías y ejecutar `gameshelf.Main` (codificación UTF-8).

## Base de datos
GameShelf guarda todo en una base de datos **H2**: usuarios, juegos, calificaciones, seguimiento e **imágenes** (como BLOB
dentro de la propia base). Por defecto es **un único archivo**, `gameshelf-db.mv.db`, en la carpeta desde la que se ejecuta
la app. Ese archivo contiene todo: **si se lo pasás a otra persona (junto con `GameShelf.jar`), ve los juegos, los usuarios
y las imágenes**, sin carpetas aparte. Usuario y contraseña de la base: `gameshelf` / `gameshelf` (internos; no son las
cuentas de la app). Varias copias de la app en la misma PC pueden usar el archivo a la vez.

### Importar los archivos de la versión anterior (JSON + carpeta `imagenes`)
- **Automático:** copiá `GameShelf.jar` a la carpeta de la versión vieja (la que tiene `gameshelf.json`, `usuarios.json`,
  `calificaciones.json`, `seguimiento.json` y `imagenes/`) y ejecutalo. Si la base está vacía, importa todo y guarda cada imagen
  dentro de la base. Se conservan los ids, las contraseñas y las fechas.
- **Manual:** `java -jar GameShelf.jar --importar "C:\ruta\carpeta_vieja"`. Es seguro repetirlo: no duplica datos, y si a la base le
  faltan imágenes y están en `imagenes/` de esa carpeta, las completa.

### Hostear la base (que otras PCs se conecten)
1. En la PC anfitriona, en la carpeta que tiene `gameshelf-db.mv.db`: `servidor.bat` / `./servidor.sh`
   (o `java -jar GameShelf.jar --servidor [--puerto 9092] [--carpeta ruta]`). Muestra las direcciones para conectarse.
   Dejá esa ventana abierta. Si hay firewall, habilitá el puerto (9092 por defecto).
2. En cada PC cliente: abrir GameShelf > **Iniciar sesión** > **Conexión...** > *Servidor de la red* > dirección de la
   anfitriona, puerto 9092, base `gameshelf-db`. La app prueba la conexión y la recuerda en `gameshelf.properties`.
   (También se puede escribir ese archivo a mano: `db.url=jdbc:h2:tcp://IP:9092/gameshelf-db`, `db.usuario`, `db.clave`.)
3. La anfitriona también usa la app conectándose a `localhost` (no abras el archivo directamente mientras el servidor corre).
Para hostear en Internet hay que abrir/redirigir el puerto del router (o usar una VPN). Quien tenga acceso a la base puede
leer todo lo que contiene: las notas y calificaciones privadas lo son dentro de la aplicación.

## Cuentas y permisos
- **Primer uso:** se crea la cuenta del **administrador**.
- Cualquiera puede **crear su cuenta** desde la pantalla de login (rol Usuario).
- **Agregar juegos:** cualquier usuario (con o sin imagen). Quedan visibles para todos.
- **Datos del juego** (nombre, plataforma, género, desarrollador, año e imagen): una vez agregado, **solo el administrador**
  puede modificarlos ("Editar datos del juego"), ni siquiera quien lo agregó. También es el **único que puede dar de baja**
  un juego (se borran su imagen y los datos personales asociados). Esos botones solo existen para él.
- **Datos personales, independientes por usuario** (nadie ve los de otros): **estado**, horas jugadas, favorito, **notas**
  ("Mi seguimiento...", que se abre también con doble clic en un juego) y **calificación** (dentro de ese mismo diálogo o con "Calificar juego..."). Un juego que el usuario nunca tocó queda **sin estado**; "Pendiente" es un estado que el usuario elige (juego que quiere jugar).
  Regla de estado: al cargar **horas de juego** a un juego que está *Pendiente* o *sin estado*, pasa solo a **Jugando** (en el
  formulario se ve en el momento; no pisa Terminado ni Abandonado, y no se repite si se guardan las mismas horas).
  La calificación se puede **cambiar** cuando se quiera (queda una sola por usuario y juego); "Calificar juego..." ofrece los
  juegos que todavía no calificaste y, si hay uno seleccionado, también ese para cambiarle la nota.
- Estadísticas, filtros por estado/favoritos/calificación y "Mi nota media" usan solo los datos propios;
  el total de juegos es común.
- Las contraseñas se guardan con PBKDF2 + sal (nunca en texto plano).

## Pestaña "Ranking de juegos"
Muestra todos los juegos ordenados por el **promedio de las calificaciones de todos los usuarios** (con la cantidad de
votos; los juegos sin calificaciones van al final). Es anónimo: solo se ven promedios, nunca la nota de cada usuario.
Tiene un **buscador por nombre** (sin distinguir mayúsculas ni tildes). Junto al promedio se ve el estado y la nota propios. El filtro "Mostrar" permite ver solo los juegos **sin estado** o los
que todavía no calificaste; desde ahí se les puede poner un estado con un clic (Pendiente, Jugando, Terminado,
Abandonado o Sin estado), abrir "Mi seguimiento...", calificarlos o marcarlos como favoritos.

## Plataformas, géneros y desarrolladores
Los tres campos admiten **varios valores separados por coma** o punto y coma (ej.: `PC, Switch` · `RPG, Aventura` ·
`Nintendo EPD, Monolith Soft`). Cada valor cuenta por separado y no se distinguen mayúsculas: `pc` y `PC` son lo mismo y
se unifican con la escritura que ya existe en la biblioteca. Un mismo juego no puede repetirse en una plataforma que ya
tenga cargada (máximo 10 valores por campo y 60 caracteres por valor).
- **Filtro por plataforma:** lista con cada plataforma por separado.
- **Filtro por géneros:** botón desplegable con una **lista deslizable de casillas**; se muestran los juegos que tengan
  **cualquiera** de los géneros marcados. Está en la pestaña Biblioteca y en el Ranking.
- **Quitar filtros:** cada pestaña tiene un único botón **"Quitar filtros"** que deja sin aplicar la búsqueda, los géneros y
  todos los demás filtros a la vez (el desplegable de géneros no tiene un botón aparte).
- **Buscador:** busca por nombre **o por desarrollador** (sin distinguir mayúsculas ni tildes), en Biblioteca y Ranking.

## Datos compartidos
Todos los usuarios que usen la **misma base** (el mismo archivo o el mismo servidor) ven la misma biblioteca; los cambios hechos por otros se
leen solos al volver a la ventana o al cambiar de pestaña (no hay botón "Actualizar").

## Imágenes
Se eligen desde la PC (PNG, JPG, GIF o BMP, hasta 5 MB y 4096x4096 px) y se guardan **dentro de la base de datos**, así que
viajan con ella y siguen disponibles aunque se mueva o borre el archivo original. Cada imagen se guarda con el **nombre del juego separado por guiones bajos** (sin tildes ni símbolos): "Donkey Kong 64" queda como
`Donkey_Kong_64.jpg`, y "Crisis Core: Final Fantasy VII" como `Crisis_Core_Final_Fantasy_VII.png`. Si otro juego tiene el mismo título
(por ejemplo en otra plataforma) se agrega un número: `Celeste_2.png`. Las imágenes que ya estaban guardadas (por ejemplo, las importadas de la
versión anterior, que tenían nombres aleatorios) se **renombran solas** al abrir la aplicación si su nombre no coincide con el de su
juego (se ignoran tildes, símbolos y mayúsculas); también cuando el administrador le cambia el nombre a un juego. Es opcional: un juego puede no tener imagen.

## Arquitectura
`model` · `persistence` (ConexionDB, Esquema, repositorios con JDBC/SQL, AlmacenImagenes, MigradorJson) · `service`
(VideojuegoService, UsuarioService, CalificacionService, SeguimientoService) · `controller` (sesión y mensajes) ·
`view` (MainFrame, RankingPanel, JuegoDialog, SeguimientoDialog, CalificarDialog, LoginDialog, RegistroDialog, ConexionDialog) ·
`Servidor` (modo servidor). Tablas: `usuario`, `videojuego`, `imagen` (BLOB), `seguimiento`, `calificacion` (claves
foráneas con borrado en cascada).

## Errores intencionales corregidos
- `ConexionDB.setUrl` rechaza null y valores vacíos.
- `VideojuegoService.agregar` valida el nombre (y año, horas, longitudes, duplicados).
- `VideojuegoController` informa la causa real del error.
- `VideojuegoRepository` guarda, edita y elimina en la base de datos.

## Licencias
Incluye el motor de base de datos H2 (`lib/h2.jar`, versión 2.2.220), con licencia MPL 2.0 / EPL 1.0: https://h2database.com
