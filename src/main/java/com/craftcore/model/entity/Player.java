package com.craftcore.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que representa a un jugador de Minecraft conocido o validado vía Mojang API.
 */
@Entity
@Table(name = "jugadores")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_jugador")
    private Integer idJugador;

    @Column(name = "uuid", length = 36, nullable = false, unique = true)
    private String uuid;

    @Column(name = "nickname", length = 16, nullable = false)
    private String nickname;

    @Column(name = "skin_url", length = 255)
    private String skinUrl;

    @Column(name = "es_op", nullable = false)
    private Boolean esOp = false;

    @Column(name = "en_whitelist", nullable = false)
    private Boolean enWhitelist = false;

    @Column(name = "primera_conexion", nullable = false)
    private LocalDateTime primeraConexion;

    @Column(name = "ultima_conexion")
    private LocalDateTime ultimaConexion;

    public Player() {
    }

    public Player(String uuid, String nickname) {
        this.uuid = uuid;
        this.nickname = nickname;
    }

    @PrePersist
    protected void onCreate() {
        if (this.primeraConexion == null) {
            this.primeraConexion = LocalDateTime.now();
        }
        if (this.esOp == null) {
            this.esOp = false;
        }
        if (this.enWhitelist == null) {
            this.enWhitelist = false;
        }
    }

    public Integer getIdJugador() {
        return idJugador;
    }

    public void setIdJugador(Integer idJugador) {
        this.idJugador = idJugador;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getSkinUrl() {
        return skinUrl;
    }

    public void setSkinUrl(String skinUrl) {
        this.skinUrl = skinUrl;
    }

    public Boolean getEsOp() {
        return esOp;
    }

    public void setEsOp(Boolean esOp) {
        this.esOp = esOp;
    }

    public Boolean getEnWhitelist() {
        return enWhitelist;
    }

    public void setEnWhitelist(Boolean enWhitelist) {
        this.enWhitelist = enWhitelist;
    }

    public LocalDateTime getPrimeraConexion() {
        return primeraConexion;
    }

    public void setPrimeraConexion(LocalDateTime primeraConexion) {
        this.primeraConexion = primeraConexion;
    }

    public LocalDateTime getUltimaConexion() {
        return ultimaConexion;
    }

    public void setUltimaConexion(LocalDateTime ultimaConexion) {
        this.ultimaConexion = ultimaConexion;
    }

    @Override
    public String toString() {
        return nickname + " [" + uuid + "]";
    }
}
