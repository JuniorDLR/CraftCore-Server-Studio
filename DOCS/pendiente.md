# ðŸ“‹ CraftCore Server Studio â€” Plan de Trabajo y Pendientes

> **Proyecto:** CraftCore Server Studio  
> **Asignatura:** ProgramaciÃ³n de Aplicaciones de Escritorio (UAM - Managua)  
> **Profesor:** Norman Cash Arcia  
> **LÃ­mite de Pantallas:** 5 Vistas Principales (Fijado por el docente)  
> **Resultados de Aprendizaje:** RA1 (JavaFX), RA2 (JDBC), RA3 (Hilos/Concurrencia), RA4 (Hibernate + JasperReports), RA5 (REST Mojang API)

---

## ðŸ“Œ Estado General del Proyecto
- [x] **Fase 1: InicializaciÃ³n, Git y ConfiguraciÃ³n Maven**
- [x] **Fase 2: Base de Datos, Entidades Hibernate y Acceso JDBC**
- [x] **Fase 3: Motor Concurrente de Minecraft (ProcessBuilder & Hilos)**
- [x] **Fase 4: Servicio Web REST (Mojang API & File I/O Whitelist)**
- [x] **Fase 5: Desarrollo de las 5 Pantallas en JavaFX (Scene Builder)**
- [x] **Fase 6: MÃ³dulo de Reportes con JasperReports (PDF y Excel)**
- [x] **Fase 7: Pruebas Integradoras, ValidaciÃ³n y DocumentaciÃ³n**

---

## ðŸ—„ï¸ Modelo de Base de Datos Relacional

A continuaciÃ³n se detalla el esquema relacional normalizado diseÃ±ado para soportar tanto **JDBC puro (RA2)** como **Hibernate ORM con relaciones (RA4)** y la reporterÃ­a de **JasperReports**:

### 1. Tabla: `administradores`
Almacena las credenciales y roles de los operadores del sistema que inician sesiÃ³n en CraftCore Server Studio.
* `id_admin` (INT, PK, AUTO_INCREMENT / SERIAL)
* `username` (VARCHAR(50), UNIQUE, NOT NULL)
* `password_hash` (VARCHAR(255), NOT NULL)
* `nombre_completo` (VARCHAR(100), NOT NULL)
* `rol` (VARCHAR(20), NOT NULL: 'SUPERADMIN', 'MODERADOR')
* `activo` (BOOLEAN, DEFAULT TRUE)
* `fecha_creacion` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

### 2. Tabla: `jugadores`
Registra a todos los usuarios conocidos, validados vÃ­a Mojang API o detectados en el servidor.
* `id_jugador` (INT, PK, AUTO_INCREMENT / SERIAL)
* `uuid` (VARCHAR(36), UNIQUE, NOT NULL) *(Ej: `8667ba71-b85a-4004-af54-457a9734eed7`)*
* `nickname` (VARCHAR(16), NOT NULL)
* `skin_url` (VARCHAR(255), NULL)
* `es_op` (BOOLEAN, DEFAULT FALSE)
* `en_whitelist` (BOOLEAN, DEFAULT FALSE)
* `primera_conexion` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
* `ultima_conexion` (TIMESTAMP, NULL)

### 3. Tabla: `sanciones` *(RelaciÃ³n ManyToOne con `jugadores` y `administradores`)*
Almacena el historial completo de moderaciÃ³n (baneos, expulsiones, advertencias) para auditorÃ­a y reportes PDF/Excel.
* `id_sancion` (INT, PK, AUTO_INCREMENT / SERIAL)
* `id_jugador` (INT, FK -> `jugadores.id_jugador`, NOT NULL)
* `id_admin` (INT, FK -> `administradores.id_admin`, NOT NULL)
* `tipo` (VARCHAR(20), NOT NULL: 'BAN', 'KICK', 'WARN', 'MUTE')
* `motivo` (VARCHAR(255), NOT NULL)
* `fecha_inicio` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
* `fecha_fin` (TIMESTAMP, NULL) *(Para baneos temporales)*
* `estado` (VARCHAR(20), DEFAULT 'ACTIVO': 'ACTIVO', 'REVOCADO', 'EXPIRADO')
* `comando_ejecutado` (VARCHAR(255), NOT NULL) *(Ej: `/ban Steve Uso de hacks`)*

### 4. Tabla: `sesiones_servidor` *(Control de tiempo de actividad del proceso Java)*
Registra cada vez que se arranca o apaga el subproceso de Minecraft mediante el `ProcessBuilder`.
* `id_sesion` (INT, PK, AUTO_INCREMENT / SERIAL)
* `id_admin` (INT, FK -> `administradores.id_admin`, NOT NULL)
* `fecha_inicio` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
* `fecha_fin` (TIMESTAMP, NULL)
* `jugadores_pico` (INT, DEFAULT 0)
* `estado_salida` (VARCHAR(30), DEFAULT 'EN_EJECUCION': 'EN_EJECUCION', 'APAGADO_NORMAL', 'CRASH')

### 5. Tabla: `logs_auditoria` *(Para demostrar consultas dinÃ¡micas y filtrado JDBC en RA2)*
Registra acciones operativas realizadas desde los botones de la interfaz.
* `id_log` (BIGINT, PK, AUTO_INCREMENT / SERIAL)
* `id_admin` (INT, FK -> `administradores.id_admin`, NULL)
* `accion` (VARCHAR(50), NOT NULL: 'INICIO_SRV', 'STOP_SRV', 'EJECUCION_COMANDO', 'WHITELIST_ADD', 'CONFIG_CAMBIO')
* `detalle` (TEXT, NOT NULL)
* `fecha` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

---

## ðŸ›ï¸ Arquitectura del Software (Capas y Paquetes)

```
com.craftcore/
â”œâ”€â”€ app/
â”‚   â”œâ”€â”€ MainApp.java                       # Inicializador JavaFX (extends Application)
â”‚   â””â”€â”€ Launcher.java                      # Entrada main alternativa para empaquetado JAR
â”œâ”€â”€ controller/                            # Controladores FXML (RA1)
â”‚   â”œâ”€â”€ DashboardController.java           # [P1] Consola en vivo, botones de estado y comandos rÃ¡pidos
â”‚   â”œâ”€â”€ LivePlayersController.java         # [P2] TableView de jugadores conectados + ContextMenu moderaciÃ³n
â”‚   â”œâ”€â”€ WhitelistController.java           # [P3] Consumo Mojang REST, ImageView de skin y whitelist.json
â”‚   â”œâ”€â”€ AuditHistoryController.java        # [P4] Tabla de sanciones, filtros DatePicker y CRUD JDBC/ORM
â”‚   â””â”€â”€ ReportsConfigController.java       # [P5] Exportador JasperReports y editor de server.properties
â”œâ”€â”€ model/
â”‚   â”œâ”€â”€ entity/                            # Entidades Hibernate JPA (RA4)
â”‚   â”‚   â”œâ”€â”€ Admin.java
â”‚   â”‚   â”œâ”€â”€ Player.java
â”‚   â”‚   â”œâ”€â”€ Sanction.java
â”‚   â”‚   â”œâ”€â”€ ServerSession.java
â”‚   â”‚   â””â”€â”€ AuditLog.java
â”‚   â””â”€â”€ dto/                               # DTOs para mapeo de API REST JSON (RA5)
â”‚       â”œâ”€â”€ MojangProfileDTO.java
â”‚       â””â”€â”€ MojangTexturesDTO.java
â”œâ”€â”€ dao/                                   # Capa de Acceso a Datos (SeparaciÃ³n limpia)
â”‚   â”œâ”€â”€ jdbc/                              # Implementaciones directas JDBC puro (RA2)
â”‚   â”‚   â””â”€â”€ AuditLogJdbcDAO.java
â”‚   â””â”€â”€ orm/                               # Implementaciones con Hibernate SessionFactory (RA4)
â”‚       â”œâ”€â”€ PlayerDAO.java
â”‚       â””â”€â”€ SanctionDAO.java
â”œâ”€â”€ service/                               # LÃ³gica de Negocio y ConexiÃ³n Externa
â”‚   â”œâ”€â”€ MinecraftProcessService.java       # Orquestador ProcessBuilder, Threads y Pipes I/O (RA3)
â”‚   â”œâ”€â”€ MojangApiService.java              # HttpClient y parsing Jackson (RA5)
â”‚   â”œâ”€â”€ ModerationService.java             # Puente 1-clic: Inyecta comando + Guarda en BD
â”‚   â”œâ”€â”€ ConfigurationFileService.java      # Parser y manipulador de server.properties y whitelist.json
â”‚   â””â”€â”€ ReportGeneratorService.java        # CompilaciÃ³n y exportaciÃ³n JasperReports (RA4)
â””â”€â”€ util/                                  # Utilidades transversales
    â”œâ”€â”€ HibernateUtil.java                 # Singleton para SessionFactory
    â”œâ”€â”€ DatabaseConnection.java            # ConexiÃ³n nativa JDBC (DriverManager)
    â””â”€â”€ AlertHelper.java                   # Ventanas emergentes Alert/Dialog reutilizables
```

---

## ðŸŽ¯ Desglose de Pendientes por Prioridad (Checklist de Desarrollo)

### Prioridad 1: ConfiguraciÃ³n Inicial del Entorno y Control de Versiones
- [x] Inicializar el repositorio Git local en `/home/Junaxer/IdeaProjects/CraftCore Server Studio`.
- [x] Configurar repositorio remoto en GitHub (`CraftCore-Server-Studio`).
- [x] Crear archivo `.gitignore` optimizado para Java, IntelliJ, Scene Builder y archivos pesados de Minecraft (`*.jar`, `world/`, `logs/`).
- [x] Crear el `pom.xml` con las dependencias requeridas:
  - JavaFX 21 (`javafx-controls`, `javafx-fxml`, `javafx-graphics`).
  - Hibernate ORM 6.x (`hibernate-core`).
  - Driver de Base de Datos (PostgreSQL o SQL Server).
  - Jackson Databind (Para serializar/deserializar JSON de Mojang y whitelist).
  - JasperReports 7.x (`jasperreports`, `jasperreports-pdf`, `poi` para Excel).
- [x] Realizar el primer commit inicial y verificar conexiÃ³n con GitHub.

### Prioridad 2: Base de Datos y Persistencia (RA2 y RA4)
- [x] Crear el script SQL DDL (`schema.sql`) para generar las 5 tablas relacionales.
- [x] Configurar el archivo `hibernate.cfg.xml` con credenciales de conexiÃ³n y mapeo de clases.
- [x] Crear la clase utilitaria `DatabaseConnection.java` para conexiÃ³n directa JDBC (RA2).
- [x] Crear la clase utilitaria `HibernateUtil.java` con `StandardServiceRegistryBuilder` (RA4).
- [x] Crear las entidades anotadas: `Admin`, `Player`, `Sanction`, `ServerSession`, `AuditLog`.
- [x] Implementar y probar mÃ©todos CRUD con Hibernate (`persist`, `find`, `merge`, `remove`).
- [x] Implementar consultas JDBC con `PreparedStatement` y `ResultSet` para filtros dinÃ¡micos.

### Prioridad 3: Motor Concurrente de Minecraft (RA3 - Core del Servidor)
- [x] Crear `MinecraftProcessService.java` que maneje el ciclo de vida del subproceso:
  - Arranque asÃ­ncrono con `ProcessBuilder("java", "-Xmx1024M", "-jar", "server.jar", "nogui")`.
  - Hilo de lectura continua (`Runnable`) del `InputStream` de la consola.
  - SincronizaciÃ³n hacia la interfaz grÃ¡fica utilizando `Platform.runLater()`.
  - Hilo de inyecciÃ³n de comandos en el `OutputStream` (`PrintWriter.println(cmd)`).
  - Procedimiento de apagado limpio (envÃ­o de `/stop` y espera con `process.waitFor()`).
- [x] Preparar un entorno de pruebas con un `server.jar` ligero (Paper/Spigot o Vanilla) y `eula.txt=true`.

### Prioridad 4: Consumo REST y Manejo de Archivos (RA5 + File I/O)
- [x] Crear `MojangApiService.java` con `java.net.http.HttpClient`:
  - Consumo de `https://api.mojang.com/users/profiles/minecraft/{username}`.
  - ExtracciÃ³n de UUID oficial y validaciÃ³n de cÃ³digo HTTP 200 vs 404.
  - Consumo del Session Server para decodificar textura de skin Base64.
- [x] Crear `ConfigurationFileService.java`:
  - Lectura y escritura atÃ³mica de `server.properties` con `java.util.Properties`.
  - Lectura y escritura de `whitelist.json` usando Jackson `ObjectMapper`.

### Prioridad 5: DiseÃ±o y ProgramaciÃ³n de las 5 Pantallas en JavaFX (RA1)
- [x] **Pantalla 1: Dashboard Central (`DashboardView.fxml`)**
  - Barra de menÃºs (`MenuBar`) y herramientas (`ToolBar`).
  - Terminal de texto monoespaciada para la consola en vivo (`TextArea`).
  - Botones de estado (Iniciar, Detener, Reiniciar) y botones de acciÃ³n rÃ¡pida en 1 clic.
- [x] **Pantalla 2: ModeraciÃ³n en Vivo (`LiveModerationView.fxml`)**
  - `TableView` con columnas de Jugador, Ping, UUID y Avatar (`ImageView`).
  - `ContextMenu` para `/kick`, `/ban`, `/op`, `/gamemode` vinculado a la base de datos.
- [x] **Pantalla 3: Whitelist y ValidaciÃ³n Mojang (`WhitelistManagerView.fxml`)**
  - Formulario con `TextField`, botÃ³n de consulta REST e `ImageView` de la skin obtenida.
  - `TableView` de jugadores autorizados en `whitelist.json` con botÃ³n de eliminaciÃ³n.
- [x] **Pantalla 4: Historial de AuditorÃ­a y CRUD BD (`AuditHistoryView.fxml`)**
  - Filtros por fecha (`DatePicker`) y tipo de evento (`ComboBox`).
  - `TableView` conectado a consultas JDBC y botones CRUD de gestiÃ³n manual.
- [x] **Pantalla 5: Reportes Jasper y ConfiguraciÃ³n (`ReportsConfigView.fxml`)**
  - Selector de formato con `RadioButton` (PDF / Excel).
  - Selector de reporte con `ComboBox` y botÃ³n de exportaciÃ³n con `FileChooser`.
  - Formulario editor de `server.properties` (`TextField`, `ComboBox`, `CheckBox`).

### Prioridad 6: Plantillas JasperReports (RA4)
- [x] Crear plantilla `reporte_sanciones.jrxml` con parÃ¡metros de fecha, logo y totales.
- [x] Crear plantilla `reporte_actividad_jugadores.jrxml`.
- [x] Integrar el servicio `ReportGeneratorService.java` para compilar y exportar a PDF y Excel.

### Prioridad 7: Pulido Final y PreparaciÃ³n de Defensa AcadÃ©mica
- [x] Validaciones de entrada en todos los formularios (`TextField` vacÃ­os, rangos numÃ©ricos).
- [x] Manejo robusto de errores con `AlertHelper` (sin caÃ­das abruptas de la aplicaciÃ³n).
- [x] Pruebas completas de flujo cruzado (Ej: Banear en P2 -> ver expulsiÃ³n en P1 -> ver registro en P4 -> generar reporte PDF en P5).
- [x] RedacciÃ³n de la memoria tÃ©cnica y preparaciÃ³n del guion para la defensa con el profesor Norman Cash.
