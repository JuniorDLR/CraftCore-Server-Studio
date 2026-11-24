package com.craftcore.service;

import com.craftcore.model.dto.WhitelistEntryDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Servicio de lectura y escritura atómica de archivos de configuración del servidor
 * (server.properties y whitelist.json).
 */
public class ConfigurationFileService {

    private final ObjectMapper objectMapper;

    public ConfigurationFileService() {
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    /**
     * Lee el archivo server.properties desde el directorio del servidor.
     */
    public Properties loadServerProperties(Path serverDirectory) throws IOException {
        Path propsPath = serverDirectory.resolve("server.properties");
        Properties properties = new Properties();
        if (!Files.exists(propsPath)) {
            return properties;
        }
        try (InputStream in = Files.newInputStream(propsPath)) {
            properties.load(in);
        }
        return properties;
    }

    /**
     * Guarda server.properties de forma atómica usando un archivo temporal.
     */
    public void saveServerProperties(Path serverDirectory, Properties properties) throws IOException {
        Files.createDirectories(serverDirectory);
        Path target = serverDirectory.resolve("server.properties");
        Path temp = Files.createTempFile(serverDirectory, "server.properties.", ".tmp");

        try {
            try (OutputStream out = Files.newOutputStream(temp)) {
                properties.store(out, "CraftCore Server Studio - server.properties");
            }
            moveAtomically(temp, target);
        } catch (IOException ex) {
            Files.deleteIfExists(temp);
            throw ex;
        }
    }

    /**
     * Lee la lista de jugadores autorizados desde whitelist.json.
     */
    public List<WhitelistEntryDTO> loadWhitelist(Path serverDirectory) throws IOException {
        Path whitelistPath = serverDirectory.resolve("whitelist.json");
        if (!Files.exists(whitelistPath) || Files.size(whitelistPath) == 0) {
            return new ArrayList<>();
        }
        return objectMapper.readValue(whitelistPath.toFile(), new TypeReference<>() {
        });
    }

    /**
     * Escribe whitelist.json de forma atómica con formato legible.
     */
    public void saveWhitelist(Path serverDirectory, List<WhitelistEntryDTO> entries) throws IOException {
        Files.createDirectories(serverDirectory);
        Path target = serverDirectory.resolve("whitelist.json");
        Path temp = Files.createTempFile(serverDirectory, "whitelist.", ".tmp");

        try {
            objectMapper.writeValue(temp.toFile(), entries);
            moveAtomically(temp, target);
        } catch (IOException ex) {
            Files.deleteIfExists(temp);
            throw ex;
        }
    }

    /**
     * Agrega un jugador a whitelist.json si no está presente (por UUID).
     */
    public List<WhitelistEntryDTO> addToWhitelist(Path serverDirectory, String formattedUuid, String name) throws IOException {
        List<WhitelistEntryDTO> entries = loadWhitelist(serverDirectory);
        boolean exists = entries.stream()
                .anyMatch(e -> formattedUuid.equalsIgnoreCase(e.getUuid()));
        if (!exists) {
            entries.add(new WhitelistEntryDTO(formattedUuid, name));
            saveWhitelist(serverDirectory, entries);
        }
        return entries;
    }

    /**
     * Elimina un jugador de whitelist.json por su UUID.
     */
    public List<WhitelistEntryDTO> removeFromWhitelist(Path serverDirectory, String uuid) throws IOException {
        List<WhitelistEntryDTO> entries = loadWhitelist(serverDirectory);
        boolean removed = entries.removeIf(e -> uuid.equalsIgnoreCase(e.getUuid()));
        if (removed) {
            saveWhitelist(serverDirectory, entries);
        }
        return entries;
    }

    private void moveAtomically(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
