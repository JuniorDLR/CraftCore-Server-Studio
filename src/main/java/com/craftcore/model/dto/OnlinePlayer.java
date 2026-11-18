package com.craftcore.model.dto;

/**
 * Modelo de vista para representar a un jugador conectado en tiempo real.
 */
public class OnlinePlayer {

    private String nickname;
    private String uuid;
    private int ping;
    private String avatarUrl;

    public OnlinePlayer(String nickname, String uuid, int ping, String avatarUrl) {
        this.nickname = nickname;
        this.uuid = uuid;
        this.ping = ping;
        this.avatarUrl = avatarUrl;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getPing() {
        return ping;
    }

    public void setPing(int ping) {
        this.ping = ping;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
