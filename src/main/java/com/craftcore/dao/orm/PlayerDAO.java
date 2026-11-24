package com.craftcore.dao.orm;

import com.craftcore.model.entity.Player;
import com.craftcore.util.HibernateUtil;

import java.util.List;
import java.util.Optional;

/**
 * DAO Hibernate para la gestión de jugadores (RA4).
 */
public class PlayerDAO extends BaseHibernateDAO<Player, Integer> {

    public PlayerDAO() {
        super(Player.class);
    }

    /**
     * Busca un jugador por su UUID oficial de Minecraft.
     */
    public Optional<Player> findByUuid(String uuid) {
        return HibernateUtil.executeQuery(session ->
                session.createQuery("FROM Player p WHERE p.uuid = :uuid", Player.class)
                        .setParameter("uuid", uuid)
                        .uniqueResultOptional()
        );
    }

    /**
     * Busca un jugador por su nickname (insensible a mayúsculas/minúsculas).
     */
    public Optional<Player> findByNickname(String nickname) {
        return HibernateUtil.executeQuery(session ->
                session.createQuery("FROM Player p WHERE LOWER(p.nickname) = LOWER(:nickname)", Player.class)
                        .setParameter("nickname", nickname)
                        .uniqueResultOptional()
        );
    }

    /**
     * Obtiene todos los jugadores habilitados en la whitelist.
     */
    public List<Player> findWhitelisted() {
        return HibernateUtil.executeQuery(session ->
                session.createQuery("FROM Player p WHERE p.enWhitelist = true ORDER BY p.nickname ASC", Player.class)
                        .list()
        );
    }
}
