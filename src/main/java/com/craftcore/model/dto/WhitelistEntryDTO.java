package com.craftcore.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO que representa una entrada del archivo whitelist.json de Minecraft.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class WhitelistEntryDTO {

    private String uuid;
    private String name;

    public WhitelistEntryDTO() {
    }

    public WhitelistEntryDTO(String uuid, String name) {
        this.uuid = uuid;
        this.name = name;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
