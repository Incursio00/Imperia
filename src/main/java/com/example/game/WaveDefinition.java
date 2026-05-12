package com.example.game;

import com.example.entities.EnemyType;

import java.util.ArrayList;
import java.util.List;

/**
 * Descrive la composizione di una singola ondata nemica.
 *
 * Ogni ondata è una lista di SpawnEntry:
 *  - tipo di nemico
 *  - ritardo in secondi dall'inizio dell'ondata prima che appaia
 *
 * Esempio ondata 1:
 *   t=0.0  → GNOME
 *   t=2.0  → GNOME
 *   t=4.0  → THIEF
 *   t=6.0  → THIEF
 */
public class WaveDefinition {

    public record SpawnEntry(EnemyType type, double spawnTime) {}

    private final int             waveNumber;
    private final List<SpawnEntry> entries;
    private final double           duration;   // durata massima ondata in secondi

    public WaveDefinition(int waveNumber, double duration) {
        this.waveNumber = waveNumber;
        this.duration   = duration;
        this.entries    = new ArrayList<>();
    }

    public WaveDefinition add(EnemyType type, double spawnTime) {
        entries.add(new SpawnEntry(type, spawnTime));
        return this; // builder pattern
    }

    // -------------------------------------------------------------------------
    // Factory — definisce tutte le ondate del gioco
    // -------------------------------------------------------------------------

    public static List<WaveDefinition> buildAllWaves() {
        List<WaveDefinition> waves = new ArrayList<>();

        // --- Ondata 1 — Solo runner veloci (60s) ---
        waves.add(new WaveDefinition(1, 60)
            .add(EnemyType.GNOME,  0.0)
            .add(EnemyType.GNOME,  3.0)
            .add(EnemyType.THIEF,  6.0)
            .add(EnemyType.GNOME,  9.0)
            .add(EnemyType.THIEF, 12.0)
            .add(EnemyType.GNOME, 15.0)
        );

        // --- Ondata 2 — Corazzati lenti (90s) ---
        waves.add(new WaveDefinition(2, 90)
            .add(EnemyType.TURTLE,  0.0)
            .add(EnemyType.GNOME,   4.0)
            .add(EnemyType.TURTLE,  8.0)
            .add(EnemyType.LIZARD, 12.0)
            .add(EnemyType.GNOME,  16.0)
            .add(EnemyType.TURTLE, 20.0)
            .add(EnemyType.LIZARD, 24.0)
        );

        // --- Ondata 3 — Attaccanti a distanza + mix (90s) ---
        waves.add(new WaveDefinition(3, 90)
            .add(EnemyType.SHAMAN,  0.0)
            .add(EnemyType.GNOME,   2.0)
            .add(EnemyType.GNOLL,   4.0)
            .add(EnemyType.TURTLE,  6.0)
            .add(EnemyType.SHAMAN,  8.0)
            .add(EnemyType.THIEF,  10.0)
            .add(EnemyType.GNOLL,  14.0)
            .add(EnemyType.LIZARD, 18.0)
        );

        // --- Ondata 4 — Ondata mista intensa (120s) ---
        waves.add(new WaveDefinition(4, 120)
            .add(EnemyType.GNOME,   0.0)
            .add(EnemyType.SHAMAN,  2.0)
            .add(EnemyType.TURTLE,  4.0)
            .add(EnemyType.GNOLL,   6.0)
            .add(EnemyType.THIEF,   8.0)
            .add(EnemyType.LIZARD, 10.0)
            .add(EnemyType.SHAMAN, 14.0)
            .add(EnemyType.GNOME,  16.0)
            .add(EnemyType.TURTLE, 20.0)
            .add(EnemyType.GNOLL,  24.0)
        );

        // --- Ondata 5 — Boss Orco (120s) ---
        waves.add(new WaveDefinition(5, 120)
            .add(EnemyType.GNOME,    0.0)
            .add(EnemyType.SHAMAN,   3.0)
            .add(EnemyType.GNOLL,    6.0)
            .add(EnemyType.TURTLE,  10.0)
            .add(EnemyType.ORC_BOSS, 20.0)  // il boss arriva dopo i minion
        );

        return waves;
    }

    // Getters
    public int              getWaveNumber() { return waveNumber; }
    public List<SpawnEntry> getEntries()    { return entries; }
    public double           getDuration()   { return duration; }
}
