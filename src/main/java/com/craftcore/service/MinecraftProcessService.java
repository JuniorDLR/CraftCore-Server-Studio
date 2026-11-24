package com.craftcore.service;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Orquestador concurrente del subproceso del servidor dedicado de Minecraft (RA3).
 * Administra ProcessBuilder, hilos de lectura/escritura de consola y sincronización con JavaFX.
 */
public class MinecraftProcessService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinecraftProcessService.class);

    public enum ServerState {
        STOPPED,
        STARTING,
        RUNNING,
        STOPPING,
        CRASHED
    }

    private static final Pattern DONE_PATTERN = Pattern.compile("Done \\([^)]+\\)! For help, type \"help\"");
    private static final Pattern JOIN_PATTERN = Pattern.compile(":\\s+([A-Za-z0-9_]{1,16})\\s+joined the game");
    private static final Pattern LEAVE_PATTERN = Pattern.compile(":\\s+([A-Za-z0-9_]{1,16})\\s+left the game");
    private static final int SHUTDOWN_TIMEOUT_SECONDS = 20;

    private final List<Consumer<String>> consoleListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<ServerState>> stateListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> playerJoinListeners = new CopyOnWriteArrayList<>();
    private final List<Consumer<String>> playerLeaveListeners = new CopyOnWriteArrayList<>();

    private final ExecutorService commandExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "mc-command-writer");
        t.setDaemon(true);
        return t;
    });

    private volatile Process serverProcess;
    private volatile PrintWriter commandWriter;
    private volatile ServerState currentState = ServerState.STOPPED;
    private Thread consoleReaderThread;

    private Path serverDirectory = Path.of("minecraft-server");
    private String jarFileName = "server.jar";
    private int maxMemoryMb = 1024;

    public void configure(Path serverDirectory, String jarFileName, int maxMemoryMb) {
        if (isRunning()) {
            throw new IllegalStateException("No se puede reconfigurar mientras el servidor está en ejecución.");
        }
        this.serverDirectory = serverDirectory;
        this.jarFileName = jarFileName;
        this.maxMemoryMb = Math.max(512, maxMemoryMb);
    }

    public Path getServerDirectory() {
        return serverDirectory;
    }

    public String getJarFileName() {
        return jarFileName;
    }

    public int getMaxMemoryMb() {
        return maxMemoryMb;
    }

    public ServerState getCurrentState() {
        return currentState;
    }

    public boolean isRunning() {
        return serverProcess != null && serverProcess.isAlive();
    }

    public void addConsoleListener(Consumer<String> listener) {
        consoleListeners.add(listener);
    }

    public void addStateListener(Consumer<ServerState> listener) {
        stateListeners.add(listener);
    }

    public void addPlayerJoinListener(Consumer<String> listener) {
        playerJoinListeners.add(listener);
    }

    public void addPlayerLeaveListener(Consumer<String> listener) {
        playerLeaveListeners.add(listener);
    }

    /**
     * Inicia el servidor de Minecraft de forma asíncrona validando que exista el JAR.
     */
    public synchronized void startServer() throws IOException {
        if (isRunning()) {
            throw new IllegalStateException("El servidor ya se encuentra en ejecución.");
        }

        Path jarPath = serverDirectory.resolve(jarFileName);
        if (!Files.exists(jarPath)) {
            throw new IOException("No se encontró el archivo del servidor en: " + jarPath.toAbsolutePath());
        }

        updateState(ServerState.STARTING);
        emitConsoleLine("[CraftCore] Iniciando servidor en " + serverDirectory.toAbsolutePath() + "...");

        ProcessBuilder builder = new ProcessBuilder(
                "java",
                "-Xmx" + maxMemoryMb + "M",
                "-Xms" + Math.min(512, maxMemoryMb) + "M",
                "-jar",
                jarFileName,
                "nogui"
        );
        builder.directory(serverDirectory.toFile());
        builder.redirectErrorStream(true);

        serverProcess = builder.start();
        commandWriter = new PrintWriter(
                new BufferedWriter(new OutputStreamWriter(serverProcess.getOutputStream(), StandardCharsets.UTF_8)),
                true
        );

        startConsoleReader(serverProcess);
    }

    private void startConsoleReader(Process process) {
        consoleReaderThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    handleConsoleLine(line);
                }
            } catch (IOException ex) {
                if (currentState != ServerState.STOPPING) {
                    LOGGER.error("Error leyendo la consola del servidor Minecraft", ex);
                    emitConsoleLine("[CraftCore ERROR] Lectura de consola interrumpida: " + ex.getMessage());
                }
            } finally {
                handleProcessExit(process);
            }
        }, "mc-console-reader");
        consoleReaderThread.setDaemon(true);
        consoleReaderThread.start();
    }

    private void handleConsoleLine(String line) {
        emitConsoleLine(line);

        if (currentState == ServerState.STARTING && DONE_PATTERN.matcher(line).find()) {
            updateState(ServerState.RUNNING);
        }

        Matcher joinMatcher = JOIN_PATTERN.matcher(line);
        if (joinMatcher.find()) {
            String player = joinMatcher.group(1);
            notifyListeners(playerJoinListeners, player);
        }

        Matcher leaveMatcher = LEAVE_PATTERN.matcher(line);
        if (leaveMatcher.find()) {
            String player = leaveMatcher.group(1);
            notifyListeners(playerLeaveListeners, player);
        }
    }

    private void handleProcessExit(Process process) {
        int exitCode = -1;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }

        commandWriter = null;
        serverProcess = null;

        ServerState finalState = (currentState == ServerState.STOPPING || exitCode == 0)
                ? ServerState.STOPPED
                : ServerState.CRASHED;

        emitConsoleLine("[CraftCore] Proceso finalizado con código de salida: " + exitCode);
        updateState(finalState);
    }

    /**
     * Envía un comando al stdin del servidor usando un hilo dedicado para no bloquear la UI.
     */
    public void sendCommand(String command) {
        if (command == null || command.isBlank()) {
            return;
        }
        String sanitized = command.startsWith("/") ? command.substring(1).trim() : command.trim();
        if (!isRunning() || commandWriter == null) {
            emitConsoleLine("[CraftCore WARN] No se pudo enviar '" + sanitized + "': el servidor está detenido.");
            return;
        }

        commandExecutor.submit(() -> {
            PrintWriter writer = commandWriter;
            if (writer != null) {
                writer.println(sanitized);
                writer.flush();
            }
        });
    }

    /**
     * Apaga el servidor de manera limpia enviando 'stop' y esperando la finalización del proceso.
     */
    public void stopServer() {
        Process process = serverProcess;
        if (process == null || !process.isAlive()) {
            updateState(ServerState.STOPPED);
            return;
        }

        updateState(ServerState.STOPPING);
        emitConsoleLine("[CraftCore] Solicitando apagado limpio (/stop)...");
        sendCommand("stop");

        Thread shutdownWaiter = new Thread(() -> {
            try {
                boolean exited = process.waitFor(SHUTDOWN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (!exited && process.isAlive()) {
                    emitConsoleLine("[CraftCore WARN] Tiempo de espera agotado. Forzando cierre del proceso...");
                    process.destroyForcibly();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }, "mc-shutdown-waiter");
        shutdownWaiter.setDaemon(true);
        shutdownWaiter.start();
    }

    /**
     * Libera recursos al cerrar la aplicación principal.
     */
    public void shutdown() {
        if (isRunning()) {
            stopServer();
        }
        commandExecutor.shutdownNow();
    }

    private void emitConsoleLine(String line) {
        notifyListeners(consoleListeners, line);
    }

    private void updateState(ServerState newState) {
        this.currentState = newState;
        notifyListeners(stateListeners, newState);
    }

    private <T> void notifyListeners(List<Consumer<T>> listeners, T payload) {
        if (listeners.isEmpty()) {
            return;
        }
        runOnUiThread(() -> {
            for (Consumer<T> listener : listeners) {
                listener.accept(payload);
            }
        });
    }

    private void runOnUiThread(Runnable task) {
        try {
            if (Platform.isFxApplicationThread()) {
                task.run();
            } else {
                Platform.runLater(task);
            }
        } catch (IllegalStateException ex) {
            // Permite ejecutar pruebas unitarias o de consola sin inicializar el Toolkit JavaFX
            task.run();
        }
    }
}
