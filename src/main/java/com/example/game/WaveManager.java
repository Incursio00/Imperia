package com.example.game;

import com.example.entities.Enemy;
import com.example.entities.EnemyType;
import com.example.map.TileMap;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * WaveManager — gestisce il ciclo delle ondate nemiche.
 *
 * Ciclo di vita:
 *  WAITING      → tra un'ondata e l'altra (countdown visibile)
 *  IN_PROGRESS  → ondata attiva, spawna nemici in base ai timer
 *  ALL_DONE     → tutte le ondate completate → VICTORY
 *
 * Il WaveManager:
 *  - mantiene la lista di Enemy attivi
 *  - spawna nemici secondo i WaveDefinition
 *  - rimuove i nemici morti (isDead())
 *  - notifica il GameManager quando il castello viene colpito
 *  - notifica il GameManager quando tutte le ondate sono finite
 */
public class WaveManager {

    public enum WaveState { WAITING, IN_PROGRESS, ALL_DONE }

    // Secondi di attesa tra un'ondata e l'altra
    private static final double WAVE_PAUSE = 8.0;

    private final List<WaveDefinition> allWaves;
    private final List<Enemy>          activeEnemies;
    private final TileMap              tileMap;
    private final double               mapOffsetX;
    private final double               mapOffsetY;
    private final WaveCallback         callback;

    private WaveState waveState      = WaveState.WAITING;
    private int       currentWaveIdx = 0;   // indice in allWaves
    private double    waveTimer      = 0;   // tempo trascorso nell'ondata corrente
    private double    pauseTimer     = 3.0; // countdown iniziale prima della prima ondata
    private int       nextSpawnIdx   = 0;   // prossima SpawnEntry da processare

    public interface WaveCallback {
        void onCastleHit(int damage);
        void onEnemyDefeated(EnemyType type);
        void onAllWavesDone();
    }

    public WaveManager(TileMap tileMap, double mapOffsetX, double mapOffsetY, WaveCallback callback) {
        this.tileMap      = tileMap;
        this.mapOffsetX   = mapOffsetX;
        this.mapOffsetY   = mapOffsetY;
        this.callback     = callback;
        this.allWaves     = WaveDefinition.buildAllWaves();
        this.activeEnemies = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    public void update(double delta) {
        switch (waveState) {
            case WAITING     -> updateWaiting(delta);
            case IN_PROGRESS -> updateInProgress(delta);
            case ALL_DONE    -> {}
        }

        // Aggiorna tutti i nemici attivi e rimuove i morti
        activeEnemies.forEach(e -> e.update(delta));
        activeEnemies.removeIf(Enemy::isDead);
    }

    private void updateWaiting(double delta) {
        pauseTimer -= delta;
        if (pauseTimer <= 0) {
            startNextWave();
        }
    }

    private void updateInProgress(double delta) {
        waveTimer += delta;

        WaveDefinition wave = allWaves.get(currentWaveIdx);

        // Spawna i nemici al momento giusto
        while (nextSpawnIdx < wave.getEntries().size()) {
            WaveDefinition.SpawnEntry entry = wave.getEntries().get(nextSpawnIdx);
            if (waveTimer >= entry.spawnTime()) {
                spawnEnemy(entry.type());
                nextSpawnIdx++;
            } else {
                break; // i prossimi hanno spawnTime maggiore
            }
        }

        // Ondata finita: tutti i nemici spawnati E tutti morti
        boolean allSpawned = nextSpawnIdx >= wave.getEntries().size();
        boolean allDead    = activeEnemies.isEmpty();

        if (allSpawned && allDead) {
            currentWaveIdx++;
            if (currentWaveIdx >= allWaves.size()) {
                waveState = WaveState.ALL_DONE;
                if (callback != null) callback.onAllWavesDone();
            } else {
                waveState  = WaveState.WAITING;
                pauseTimer = WAVE_PAUSE;
            }
        }
    }

    private void startNextWave() {
        waveState    = WaveState.IN_PROGRESS;
        waveTimer    = 0;
        nextSpawnIdx = 0;
        System.out.println("[WaveManager] Ondata " + (currentWaveIdx + 1) + " iniziata!");
    }

    // -------------------------------------------------------------------------
    // SPAWN
    // -------------------------------------------------------------------------

    private void spawnEnemy(EnemyType type) {
        // Posizione di spawn: primo waypoint della mappa
        List<int[]> waypoints = tileMap.getWaypoints();
        int[] spawnWP = waypoints.get(0);
        double spawnX = mapOffsetX + spawnWP[0] * TileMap.RENDER_SIZE + TileMap.RENDER_SIZE / 2.0;
        double spawnY = mapOffsetY + spawnWP[1] * TileMap.RENDER_SIZE + TileMap.RENDER_SIZE / 2.0;

        Enemy enemy = new Enemy(spawnX, spawnY, type, waypoints, mapOffsetX, mapOffsetY,
            new Enemy.EnemyCallback() {
                @Override public void onReachedCastle(int damage) {
                    if (callback != null) callback.onCastleHit(damage);
                }
                @Override public void onDefeated(EnemyType defeatedType) {
                    if (callback != null) callback.onEnemyDefeated(defeatedType);
                }
            }
        );

        activeEnemies.add(enemy);
        System.out.println("[WaveManager] Spawned " + type + " @ " + spawnX + "," + spawnY);
    }

    // -------------------------------------------------------------------------
    // RENDER
    // -------------------------------------------------------------------------

    public void render(GraphicsContext gc) {
        // Renderizza tutti i nemici
        activeEnemies.forEach(e -> e.render(gc));

        // Countdown tra ondate
        if (waveState == WaveState.WAITING && currentWaveIdx < allWaves.size()) {
            renderWaveCountdown(gc);
        }
    }

    private void renderWaveCountdown(GraphicsContext gc) {
        int seconds = (int) Math.ceil(pauseTimer);
        int nextWave = currentWaveIdx + 1;

        gc.setFill(Color.web("#00000088"));
        gc.fillRoundRect(340, 20, 320, 50, 10, 10);

        gc.setFill(Color.web("#FFD700"));
        gc.setFont(Font.font("Serif", 16));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("⚔ ONDATA " + nextWave + " in arrivo tra " + seconds + "s ⚔", 500, 50);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public List<Enemy> getActiveEnemies()  { return activeEnemies; }
    public WaveState   getWaveState()      { return waveState; }
    public int         getCurrentWave()    { return currentWaveIdx + 1; }
    public int         getTotalWaves()     { return allWaves.size(); }
    public double      getPauseTimer()     { return pauseTimer; }
}
