package com.example.entities;

import com.example.map.TileMap;
import com.example.utils.Vec2;

import java.util.List;

/**
 * PathFollower — componente di movimento lungo i waypoint.
 *
 * Ogni Enemy possiede un PathFollower. Ad ogni update() il follower:
 *  1. Guarda il prossimo waypoint nella lista
 *  2. Si muove verso di esso alla velocità del nemico
 *  3. Quando è abbastanza vicino, passa al waypoint successivo
 *  4. Quando raggiunge l'ultimo waypoint (castello), notifica l'Enemy
 *
 * I waypoint sono in coordinate GRIGLIA {col, row}.
 * Il PathFollower li converte in pixel usando RENDER_SIZE e gli offset.
 */
public class PathFollower {

    private static final double ARRIVE_DIST = 3.0; // pixel — soglia di "arrivato al waypoint"

    private final List<int[]> waypoints;
    private final double      offsetX;
    private final double      offsetY;

    private int     currentWPIndex = 0;
    private boolean reachedEnd     = false;

    public PathFollower(List<int[]> waypoints, double offsetX, double offsetY) {
        this.waypoints = waypoints;
        this.offsetX   = offsetX;
        this.offsetY   = offsetY;
    }

    /**
     * Muove l'entità lungo il percorso.
     * @param entity entità da muovere
     * @param speed  pixel al secondo
     * @param delta  delta time in secondi
     */
    public void update(Entity entity, double speed, double delta) {
        if (reachedEnd || currentWPIndex >= waypoints.size()) {
            reachedEnd = true;
            return;
        }

        Vec2 target = getWaypointPixel(currentWPIndex);
        Vec2 dir    = target.sub(entity.pos).normalize();
        entity.pos  = entity.pos.add(dir.scale(speed * delta));

        // Controlla se siamo arrivati al waypoint corrente
        if (entity.pos.distanceTo(target) < ARRIVE_DIST) {
            entity.pos = target; // snap preciso
            currentWPIndex++;
            if (currentWPIndex >= waypoints.size()) {
                reachedEnd = true;
            }
        }
    }

    /** Converte le coordinate griglia del waypoint in pixel */
    private Vec2 getWaypointPixel(int index) {
        int[] wp = waypoints.get(index);
        return new Vec2(
            offsetX + wp[0] * TileMap.RENDER_SIZE + TileMap.RENDER_SIZE / 2.0,
            offsetY + wp[1] * TileMap.RENDER_SIZE + TileMap.RENDER_SIZE / 2.0
        );
    }

    public boolean hasReachedEnd()   { return reachedEnd; }
    public int     getCurrentWPIndex() { return currentWPIndex; }

    /** Direzione normalizzata verso il prossimo waypoint (utile per animazioni) */
    public Vec2 getCurrentDirection(Entity entity) {
        if (reachedEnd || currentWPIndex >= waypoints.size()) return Vec2.ZERO;
        return getWaypointPixel(currentWPIndex).sub(entity.pos).normalize();
    }
}
