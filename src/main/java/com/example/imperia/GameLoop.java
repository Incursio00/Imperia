package com.example.imperia;

import javafx.animation.AnimationTimer;

public class GameLoop extends AnimationTimer {

    private static final double MAX_DELTA = 0.05; // 50ms per evitare spiral of death

    private final GameCommand gameCommand;
    private long lastTime = -1;

    public GameLoop (GameCommand gameCommand) {
        this.gameCommand = gameCommand;
    }

    @Override
    public void handle(long nowNanos) {
        if (lastTime < 0) {
            lastTime = nowNanos;
            return;
        }

        double delta = (nowNanos - lastTime) / 1_000_000_000;
        lastTime = nowNanos;

        if (delta > MAX_DELTA) delta = MAX_DELTA; // se la finestra viene trascinata non c'è perdita di frame

        gameCommand.update(delta);
        gameCommand.render();

    }
}
