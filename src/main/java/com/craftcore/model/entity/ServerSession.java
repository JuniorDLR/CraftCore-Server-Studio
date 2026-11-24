package com.craftcore.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entidad JPA que registra cada sesión de ejecución del subproceso del servidor Minecraft.
 */
@Entity
@Table(name = "sesiones_servidor")
public class ServerSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Integer idSesion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_admin", nullable = false)
    private Admin admin;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "jugadores_pico", nullable = false)
    private Integer jugadoresPico = 0;

    @Column(name = "estado_salida", length = 30, nullable = false)
    private String estadoSalida = "EN_EJECUCION";

    public ServerSession() {
    }

    public ServerSession(Admin admin) {
        this.admin = admin;
    }

    @PrePersist
    protected void onCreate() {
        if (this.fechaInicio == null) {
            this.fechaInicio = LocalDateTime.now();
        }
        if (this.jugadoresPico == null) {
            this.jugadoresPico = 0;
        }
        if (this.estadoSalida == null) {
            this.estadoSalida = "EN_EJECUCION";
        }
    }

    public Integer getIdSesion() {
        return idSesion;
    }

    public void setIdSesion(Integer idSesion) {
        this.idSesion = idSesion;
    }

    public Admin getAdmin() {
        return admin;
    }

    public void setAdmin(Admin admin) {
        this.admin = admin;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Integer getJugadoresPico() {
        return jugadoresPico;
    }

    public void setJugadoresPico(Integer jugadoresPico) {
        this.jugadoresPico = jugadoresPico;
    }

    public String getEstadoSalida() {
        return estadoSalida;
    }

    public void setEstadoSalida(String estadoSalida) {
        this.estadoSalida = estadoSalida;
    }
}
