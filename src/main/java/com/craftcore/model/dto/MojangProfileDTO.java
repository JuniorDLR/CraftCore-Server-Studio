package com.craftcore.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO para mapear la respuesta de perfil de usuario de la API REST de Mojang (RA5).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MojangProfileDTO {

    private String id;
    private String name;
    private String skinUrl;

    public MojangProfileDTO() {
    }

    public MojangProfileDTO(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSkinUrl() {
        return skinUrl;
    }

    public void setSkinUrl(String skinUrl) {
        this.skinUrl = skinUrl;
    }

    /**
     * Devuelve el UUID en formato estándar de 36 caracteres con guiones (8-4-4-4-12).
     */
    public String getFormattedUuid() {
        if (id == null || id.length() != 32) {
            return id;
        }
        return id.substring(0, 8) + "-"
                + id.substring(8, 12) + "-"
                + id.substring(12, 16) + "-"
                + id.substring(16, 20) + "-"
                + id.substring(20, 32);
    }
}
