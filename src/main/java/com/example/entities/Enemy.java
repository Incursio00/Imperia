package com.example.entities;

import com.example.map.TileMap;
import com.example.utils.Vec2;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.List;

/**
 * Nemico base — si muove lungo i waypoint verso il castello.
 *
 * Stati:
 *  WALKING  → si muove lungo il percorso tramite PathFollower
 *  DUELING  → bloccato da un Guerriero (commit #06)
 *  DEAD     → animazione morte, poi rimosso dal WaveManager
 *
 * Quando raggiunge l'ultimo waypoint (castello):
 *  → infligge damage al castello e muore
 *  → il GameManager riduce la LiveBar
 */
public class Enemy extends Entity {

    public enum EnemyState { WALKING, DUELING, DEAD }

    private final EnemyType   type;
    private final PathFollower pathFollower;
    private EnemyState        enemyState = EnemyState.WALKING;

    // Timer animazione morte
    private double deathTimer = 0;
    private static final double DEATH_ANIM_DURATION = 0.5;

    // Callback per notificare il castello raggiunto o la morte
    private final EnemyCallback callback;

    public interface EnemyCallback {
        void onReachedCastle(int damage);
        void onDefeated(EnemyType type);
    }

    public Enemy(double startX, double startY,
                 EnemyType type,
                 List<int[]> waypoints,
                 double offsetX, double offsetY,
                 EnemyCallback callback) {

        super(startX, startY, type.hp);
        this.type         = type;
        this.callback     = callback;
        this.pathFollower = new PathFollower(waypoints, offsetX, offsetY);
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    public void update(double delta) {
        switch (enemyState) {
            case WALKING -> updateWalking(delta);
            case DEAD    -> updateDeath(delta);
            default      -> {} // DUELING: bloccato, il Guerriero gestisce
        }
    }

    private void updateWalking(double delta) {
        pathFollower.update(this, type.speed, delta);

        if (pathFollower.hasReachedEnd()) {
            // Ha raggiunto il castello!
            if (callback != null) callback.onReachedCastle(type.damage);
            alive = false;
        }
    }

    private void updateDeath(double delta) {
        deathTimer += delta;
        if (deathTimer >= DEATH_ANIM_DURATION) {
            alive = false; // il WaveManager lo rimuove
        }
    }

    // -------------------------------------------------------------------------
    // DANNO
    // -------------------------------------------------------------------------

    @Override
    public void takeDamage(int amount) {
        super.takeDamage(amount);
        if (hp <= 0 && enemyState != EnemyState.DEAD) {
            enemyState = EnemyState.DEAD;
            deathTimer = 0;
            if (callback != null) callback.onDefeated(type);
        }
    }

    // -------------------------------------------------------------------------
    // RENDER
    // -------------------------------------------------------------------------

    @Override
    public void render(GraphicsContext gc) {
        double size = TileMap.RENDER_SIZE * 0.75;

        if (enemyState == EnemyState.DEAD) {
            // Animazione morte: il nemico si rimpicciolisce e svanisce
            double alpha = 1.0 - (deathTimer / DEATH_ANIM_DURATION);
            double s     = size * alpha;
            gc.setGlobalAlpha(alpha);
            gc.setFill(Color.web("#FF4444"));
            gc.fillOval(pos.x - s / 2, pos.y - s / 2, s, s);
            gc.setGlobalAlpha(1.0);
            return;
        }

        // Colore in base al tipo
        Color bodyColor = switch (type) {
            case GNOME    -> Color.web("#88CC44");
            case THIEF    -> Color.web("#CC8844");
            case TURTLE   -> Color.web("#448844");
            case LIZARD   -> Color.web("#44AA44");
            case SHAMAN   -> Color.web("#AA44CC");
            case GNOLL    -> Color.web("#CC6644");
            case ORC_BOSS -> Color.web("#CC2222");
        };

        // Corpo
        gc.setFill(bodyColor);
        if (type.isBoss()) {
            // Boss più grande
            gc.fillRect(pos.x - size * 0.75, pos.y - size * 0.75, size * 1.5, size * 1.5);
        } else {
            gc.fillOval(pos.x - size / 2, pos.y - size / 2, size, size);
        }

        // Icona tipo (placeholder testo)
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font(7));
        gc.setTextAlign(TextAlignment.CENTER);
        String icon = switch (type) {
            case GNOME    -> "G";
            case THIEF    -> "L";
            case TURTLE   -> "T";
            case LIZARD   -> "Li";
            case SHAMAN   -> "Sc";
            case GNOLL    -> "Gn";
            case ORC_BOSS -> "ORC";
        };
        gc.fillText(icon, pos.x, pos.y + 3);

        // HP bar — sempre visibile
        renderHpBar(gc, size, size / 2 + 5);

        // Indicatore DUELING
        if (enemyState == EnemyState.DUELING) {
            gc.setStroke(Color.web("#FF4444"));
            gc.setLineWidth(1);
            gc.strokeOval(pos.x - size / 2 - 2, pos.y - size / 2 - 2, size + 4, size + 4);
        }
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public EnemyType  getType()        { return type; }
    public EnemyState getEnemyState()  { return enemyState; }
    public boolean    isDead()         { return enemyState == EnemyState.DEAD && deathTimer >= DEATH_ANIM_DURATION; }
    public boolean    isDueling()      { return enemyState == EnemyState.DUELING; }

    /** Blocca il nemico (chiamato dal Guerriero) */
    public void startDuel()  { if (enemyState == EnemyState.WALKING) enemyState = EnemyState.DUELING; }
    /** Sblocca il nemico (Guerriero morto o allontanato) */
    public void endDuel()    { if (enemyState == EnemyState.DUELING) enemyState = EnemyState.WALKING; }
}
