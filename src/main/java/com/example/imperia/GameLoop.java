package com.example.imperia;

import javafx.animation.AnimationTimer;

/**
 * Game loop basato su AnimationTimer di JavaFX.
 * Calcola il delta time in secondi e lo passa al GameManager.
 * Target: ~60 FPS (JavaFX gestisce la sincronizzazione con il monitor).
 */
public class GameLoop extends AnimationTimer {

    private static final double MAX_DELTA = 0.05; // cap a 50ms per evitare "spiral of death"

    private final GameManager gameManager;
    private long lastTime = -1;

    public GameLoop(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void handle(long nowNanos) {
        if (lastTime < 0) {
            lastTime = nowNanos;
            return;
        }

        double delta = (nowNanos - lastTime) / 1_000_000_000.0;
        lastTime = nowNanos;

        // Cap delta per evitare salti enormi se la finestra viene trascinata/ridotta
        if (delta > MAX_DELTA) delta = MAX_DELTA;

        gameManager.update(delta);
        gameManager.render();
    }
}
