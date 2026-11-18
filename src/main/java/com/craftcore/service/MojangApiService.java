package com.craftcore.service;

import com.craftcore.model.dto.MojangProfileDTO;
import com.craftcore.model.dto.MojangTexturesDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Servicio de consumo REST hacia la API oficial de Mojang para validar jugadores y skins (RA5).
 * Incluye caché en memoria y respaldo automático ante límites de tasa (HTTP 429).
 */
public class MojangApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MojangApiService.class);
    private static final String PROFILE_URL = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String SESSION_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private static final String FALLBACK_URL = "https://playerdb.co/api/player/minecraft/";
    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final Map<String, MojangProfileDTO> profileCache = new ConcurrentHashMap<>();

    public MojangApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Consulta el perfil oficial de un jugador por su nickname.
     * Retorna Optional.empty() si el jugador no existe (HTTP 404).
     */
    public Optional<MojangProfileDTO> fetchPlayerProfile(String username) throws IOException, InterruptedException {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }

        String normalizedKey = username.trim().toLowerCase();
        MojangProfileDTO cached = profileCache.get(normalizedKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        String encodedName = URLEncoder.encode(username.trim(), StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(PROFILE_URL + encodedName))
                .timeout(TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404 || response.statusCode() == 204) {
            return Optional.empty();
        }

        if (response.statusCode() == 429) {
            LOGGER.warn("Mojang API devolvió HTTP 429 (Too Many Requests) para {}. Usando respaldo PlayerDB.", username);
            Optional<MojangProfileDTO> fallback = fetchFromFallback(encodedName);
            fallback.ifPresent(dto -> profileCache.put(normalizedKey, dto));
            return fallback;
        }

        if (response.statusCode() != 200) {
            throw new IOException("Error consultando API de Mojang (HTTP " + response.statusCode() + ")");
        }

        MojangProfileDTO profile = objectMapper.readValue(response.body(), MojangProfileDTO.class);
        fetchSkinUrl(profile.getId()).ifPresent(profile::setSkinUrl);
        profileCache.put(normalizedKey, profile);
        return Optional.of(profile);
    }

    private Optional<MojangProfileDTO> fetchFromFallback(String encodedName) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(FALLBACK_URL + encodedName))
                .header("User-Agent", "CraftCore-Server-Studio/1.0")
                .timeout(TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404 || response.statusCode() == 400) {
            return Optional.empty();
        }
        if (response.statusCode() != 200) {
            throw new IOException("Mojang API alcanzó su límite temporal (HTTP 429) y el respaldo no respondió (HTTP "
                    + response.statusCode() + "). Esperá unos segundos e intentá de nuevo.");
        }

        JsonNode root = objectMapper.readTree(response.body());
        if (!root.path("success").asBoolean(false)) {
            return Optional.empty();
        }

        JsonNode playerNode = root.path("data").path("player");
        String rawId = playerNode.path("raw_id").asText("");
        String name = playerNode.path("username").asText("");
        if (rawId.isBlank() || name.isBlank()) {
            return Optional.empty();
        }

        MojangProfileDTO dto = new MojangProfileDTO(rawId, name);
        String avatar = playerNode.path("avatar").asText("");
        if (!avatar.isBlank()) {
            dto.setSkinUrl(avatar);
        }
        return Optional.of(dto);
    }

    /**
     * Consulta el Session Server de Mojang y decodifica la textura Base64 para obtener la URL de la skin.
     */
    public Optional<String> fetchSkinUrl(String rawUuid) {
        if (rawUuid == null || rawUuid.isBlank()) {
            return Optional.empty();
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(SESSION_URL + rawUuid.replace("-", "")))
                    .timeout(TIMEOUT)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return Optional.empty();
            }

            MojangTexturesDTO texturesDto = objectMapper.readValue(response.body(), MojangTexturesDTO.class);
            if (texturesDto.getProperties() == null) {
                return Optional.empty();
            }

            for (MojangTexturesDTO.Property prop : texturesDto.getProperties()) {
                if ("textures".equals(prop.getName()) && prop.getValue() != null) {
                    byte[] decoded = Base64.getDecoder().decode(prop.getValue());
                    JsonNode root = objectMapper.readTree(decoded);
                    JsonNode skinNode = root.path("textures").path("SKIN").path("url");
                    if (!skinNode.isMissingNode() && !skinNode.asText().isBlank()) {
                        return Optional.of(skinNode.asText());
                    }
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("No se pudo obtener la skin para UUID {}: {}", rawUuid, ex.getMessage());
        }
        return Optional.empty();
    }
}
