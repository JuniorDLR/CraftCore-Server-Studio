package com.craftcore.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * DTO para mapear el perfil de sesión de Mojang y sus propiedades de textura Base64 (RA5).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MojangTexturesDTO {

    private String id;
    private String name;
    private List<Property> properties;

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

    public List<Property> getProperties() {
        return properties;
    }

    public void setProperties(List<Property> properties) {
        this.properties = properties;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Property {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
}
