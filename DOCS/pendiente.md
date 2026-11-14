# 📋 CraftCore Server Studio — Plan de Trabajo y Pendientes

> **Proyecto:** CraftCore Server Studio  
> **Asignatura:** Programación de Aplicaciones de Escritorio (UAM - Managua)  
> **Profesor:** Norman Cash Arcia  
> **Límite de Pantallas:** 5 Vistas Principales (Fijado por el docente)  
> **Resultados de Aprendizaje:** RA1 (JavaFX), RA2 (JDBC), RA3 (Hilos/Concurrencia), RA4 (Hibernate + JasperReports), RA5 (REST Mojang API)

---

## 📌 Estado General del Proyecto
- [ ] **Fase 1: Inicialización, Git y Configuración Maven**
- [ ] **Fase 2: Base de Datos, Entidades Hibernate y Acceso JDBC**
- [ ] **Fase 3: Motor Concurrente de Minecraft (ProcessBuilder & Hilos)**
- [ ] **Fase 4: Servicio Web REST (Mojang API & File I/O Whitelist)**
- [ ] **Fase 5: Desarrollo de las 5 Pantallas en JavaFX (Scene Builder)**
- [ ] **Fase 6: Módulo de Reportes con JasperReports (PDF y Excel)**
- [ ] **Fase 7: Pruebas Integradoras, Validación y Documentación**

---

## 🗄️ Modelo de Base de Datos Relacional

A continuación se detalla el esquema relacional normalizado diseñado para soportar tanto **JDBC puro (RA2)** como **Hibernate ORM con relaciones (RA4)** y la reportería de **JasperReports**:

### 1. Tabla: `administradores`
Almacena las credenciales y roles de los operadores del sistema que inician sesión en CraftCore Server Studio.
* `id_admin` (INT, PK, AUTO_INCREMENT / SERIAL)
* `username` (VARCHAR(50), UNIQUE, NOT NULL)
* `password_hash` (VARCHAR(255), NOT NULL)
* `nombre_completo` (VARCHAR(100), NOT NULL)
* `rol` (VARCHAR(20), NOT NULL: 'SUPERADMIN', 'MODERADOR')
* `activo` (BOOLEAN, DEFAULT TRUE)
* `fecha_creacion` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

### 2. Tabla: `jugadores`
Registra a todos los usuarios conocidos, validados vía Mojang API o detectados en el servidor.
* `id_jugador` (INT, PK, AUTO_INCREMENT / SERIAL)
* `uuid` (VARCHAR(36), UNIQUE, NOT NULL) *(Ej: `8667ba71-b85a-4004-af54-457a9734eed7`)*
* `nickname` (VARCHAR(16), NOT NULL)
* `skin_url` (VARCHAR(255), NULL)
* `es_op` (BOOLEAN, DEFAULT FALSE)
* `en_whitelist` (BOOLEAN, DEFAULT FALSE)
* `primera_conexion` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)
* `ultima_conexion` (TIMESTAMP, NULL)

### 3. Tabla: `sanciones` *(Relación ManyToOne con `jugadores` y `administradores`)*
Almacena el historial completo de moderación (baneos, expulsiones, advertencias) para auditoría y reportes PDF/Excel.
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

### 5. Tabla: `logs_auditoria` *(Para demostrar consultas dinámicas y filtrado JDBC en RA2)*
Registra acciones operativas realizadas desde los botones de la interfaz.
* `id_log` (BIGINT, PK, AUTO_INCREMENT / SERIAL)
* `id_admin` (INT, FK -> `administradores.id_admin`, NULL)
* `accion` (VARCHAR(50), NOT NULL: 'INICIO_SRV', 'STOP_SRV', 'EJECUCION_COMANDO', 'WHITELIST_ADD', 'CONFIG_CAMBIO')
* `detalle` (TEXT, NOT NULL)
* `fecha` (TIMESTAMP, DEFAULT CURRENT_TIMESTAMP)

---

## 🏛️ Arquitectura del Software (Capas y Paquetes)

```
com.craftcore/
├── app/
│   ├── MainApp.java                       # Inicializador JavaFX (extends Application)
│   └── Launcher.java                      # Entrada main alternativa para empaquetado JAR
├── controller/                            # Controladores FXML (RA1)
│   ├── DashboardController.java           # [P1] Consola en vivo, botones de estado y comandos rápidos
│   ├── LivePlayersController.java         # [P2] TableView de jugadores conectados + ContextMenu moderación
│   ├── WhitelistController.java           # [P3] Consumo Mojang REST, ImageView de skin y whitelist.json
│   ├── AuditHistoryController.java        # [P4] Tabla de sanciones, filtros DatePicker y CRUD JDBC/ORM
│   └── ReportsConfigController.java       # [P5] Exportador JasperReports y editor de server.properties
├── model/
│   ├── entity/                            # Entidades Hibernate JPA (RA4)
│   │   ├── Admin.java
│   │   ├── Player.java
│   │   ├── Sanction.java
│   │   ├── ServerSession.java
│   │   └── AuditLog.java
│   └── dto/                               # DTOs para mapeo de API REST JSON (RA5)
│       ├── MojangProfileDTO.java
│       └── MojangTexturesDTO.java
├── dao/                                   # Capa de Acceso a Datos (Separación limpia)
│   ├── jdbc/                              # Implementaciones directas JDBC puro (RA2)
│   │   └── AuditLogJdbcDAO.java
│   └── orm/                               # Implementaciones con Hibernate SessionFactory (RA4)
│       ├── PlayerDAO.java
│       └── SanctionDAO.java
├── service/                               # Lógica de Negocio y Conexión Externa
│   ├── MinecraftProcessService.java       # Orquestador ProcessBuilder, Threads y Pipes I/O (RA3)
│   ├── MojangApiService.java              # HttpClient y parsing Jackson (RA5)
│   ├── ModerationService.java             # Puente 1-clic: Inyecta comando + Guarda en BD
│   ├── ConfigurationFileService.java      # Parser y manipulador de server.properties y whitelist.json
│   └── ReportGeneratorService.java        # Compilación y exportación JasperReports (RA4)
└── util/                                  # Utilidades transversales
    ├── HibernateUtil.java                 # Singleton para SessionFactory
    ├── DatabaseConnection.java            # Conexión nativa JDBC (DriverManager)
    └── AlertHelper.java                   # Ventanas emergentes Alert/Dialog reutilizables
```

---

## 🎯 Desglose de Pendientes por Prioridad (Checklist de Desarrollo)

### Prioridad 1: Configuración Inicial del Entorno y Control de Versiones
- [x] Inicializar el repositorio Git local en `/home/Junaxer/IdeaProjects/CraftCore Server Studio`.
- [x] Configurar repositorio remoto en GitHub (`CraftCore-Server-Studio`).
- [ ] Crear archivo `.gitignore` optimizado para Java, IntelliJ, Scene Builder y archivos pesados de Minecraft (`*.jar`, `world/`, `logs/`).
- [ ] Crear el `pom.xml` con las dependencias requeridas:
  - JavaFX 21 (`javafx-controls`, `javafx-fxml`, `javafx-graphics`).
  - Hibernate ORM 6.x (`hibernate-core`).
  - Driver de Base de Datos (PostgreSQL o SQL Server).
  - Jackson Databind (Para serializar/deserializar JSON de Mojang y whitelist).
  - JasperReports 7.x (`jasperreports`, `jasperreports-pdf`, `poi` para Excel).
- [ ] Realizar el primer commit inicial y verificar conexión con GitHub.

### Prioridad 2: Base de Datos y Persistencia (RA2 y RA4)
- [ ] Crear el script SQL DDL (`schema.sql`) para generar las 5 tablas relacionales.
- [ ] Configurar el archivo `hibernate.cfg.xml` con credenciales de conexión y mapeo de clases.
- [ ] Crear la clase utilitaria `DatabaseConnection.java` para conexión directa JDBC (RA2).
- [ ] Crear la clase utilitaria `HibernateUtil.java` con `StandardServiceRegistryBuilder` (RA4).
- [ ] Crear las entidades anotadas: `Admin`, `Player`, `Sanction`, `ServerSession`, `AuditLog`.
- [ ] Implementar y probar métodos CRUD con Hibernate (`persist`, `find`, `merge`, `remove`).
- [ ] Implementar consultas JDBC con `PreparedStatement` y `ResultSet` para filtros dinámicos.

### Prioridad 3: Motor Concurrente de Minecraft (RA3 - Core del Servidor)
- [ ] Crear `MinecraftProcessService.java` que maneje el ciclo de vida del subproceso:
  - Arranque asíncrono con `ProcessBuilder("java", "-Xmx1024M", "-jar", "server.jar", "nogui")`.
  - Hilo de lectura continua (`Runnable`) del `InputStream` de la consola.
  - Sincronización hacia la interfaz gráfica utilizando `Platform.runLater()`.
  - Hilo de inyección de comandos en el `OutputStream` (`PrintWriter.println(cmd)`).
  - Procedimiento de apagado limpio (envío de `/stop` y espera con `process.waitFor()`).
- [ ] Preparar un entorno de pruebas con un `server.jar` ligero (Paper/Spigot o Vanilla) y `eula.txt=true`.

### Prioridad 4: Consumo REST y Manejo de Archivos (RA5 + File I/O)
- [ ] Crear `MojangApiService.java` con `java.net.http.HttpClient`:
  - Consumo de `https://api.mojang.com/users/profiles/minecraft/{username}`.
  - Extracción de UUID oficial y validación de código HTTP 200 vs 404.
  - Consumo del Session Server para decodificar textura de skin Base64.
- [ ] Crear `ConfigurationFileService.java`:
  - Lectura y escritura atómica de `server.properties` con `java.util.Properties`.
  - Lectura y escritura de `whitelist.json` usando Jackson `ObjectMapper`.

### Prioridad 5: Diseño y Programación de las 5 Pantallas en JavaFX (RA1)
- [ ] **Pantalla 1: Dashboard Central (`DashboardView.fxml`)**
  - Barra de menús (`MenuBar`) y herramientas (`ToolBar`).
  - Terminal de texto monoespaciada para la consola en vivo (`TextArea`).
  - Botones de estado (Iniciar, Detener, Reiniciar) y botones de acción rápida en 1 clic.
- [ ] **Pantalla 2: Moderación en Vivo (`LiveModerationView.fxml`)**
  - `TableView` con columnas de Jugador, Ping, UUID y Avatar (`ImageView`).
  - `ContextMenu` para `/kick`, `/ban`, `/op`, `/gamemode` vinculado a la base de datos.
- [ ] **Pantalla 3: Whitelist y Validación Mojang (`WhitelistManagerView.fxml`)**
  - Formulario con `TextField`, botón de consulta REST e `ImageView` de la skin obtenida.
  - `TableView` de jugadores autorizados en `whitelist.json` con botón de eliminación.
- [ ] **Pantalla 4: Historial de Auditoría y CRUD BD (`AuditHistoryView.fxml`)**
  - Filtros por fecha (`DatePicker`) y tipo de evento (`ComboBox`).
  - `TableView` conectado a consultas JDBC y botones CRUD de gestión manual.
- [ ] **Pantalla 5: Reportes Jasper y Configuración (`ReportsConfigView.fxml`)**
  - Selector de formato con `RadioButton` (PDF / Excel).
  - Selector de reporte con `ComboBox` y botón de exportación con `FileChooser`.
  - Formulario editor de `server.properties` (`TextField`, `ComboBox`, `CheckBox`).

### Prioridad 6: Plantillas JasperReports (RA4)
- [ ] Crear plantilla `reporte_sanciones.jrxml` con parámetros de fecha, logo y totales.
- [ ] Crear plantilla `reporte_actividad_jugadores.jrxml`.
- [ ] Integrar el servicio `ReportGeneratorService.java` para compilar y exportar a PDF y Excel.

### Prioridad 7: Pulido Final y Preparación de Defensa Académica
- [ ] Validaciones de entrada en todos los formularios (`TextField` vacíos, rangos numéricos).
- [ ] Manejo robusto de errores con `AlertHelper` (sin caídas abruptas de la aplicación).
- [ ] Pruebas completas de flujo cruzado (Ej: Banear en P2 -> ver expulsión en P1 -> ver registro en P4 -> generar reporte PDF en P5).
- [ ] Redacción de la memoria técnica y preparación del guion para la defensa con el profesor Norman Cash.
