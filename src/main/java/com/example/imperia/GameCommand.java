package com.example.imperia;

import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;

public class GameCommand {

    private final GraphicsContext gc;
    private final Scene scene;

    private  GameState state = GameState.MAIN_MENU;

    private double mouseX, mouseY;
    private KeyCode keyCode;

    public GameCommand(GraphicsContext gc, Scene scene) {
        this.gc = gc;
        this.scene = scene;
        registerInput();
    }

    private void registerInput(){

        //movimento mouse
        scene.setOnMouseMoved(e -> {
            mouseX = e.getX();
            mouseY = e.getY();
        });

        scene.setOnMouseClicked(e -> {
            if ((e.getButton() == MouseButton.PRIMARY)) {
                handleLeftClick(e.getX(), e.getY());
            }
        });

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == keyCode.ESCAPE) {
                if (state == GameState.PLAYING)  state = GameState.PAUSED;
                else if ( state == GameState.PAUSED) state = GameState.PLAYING;
                }
            if (e.getCode() == keyCode.ENTER && state == GameState.MAIN_MENU){
                state = GameState.PLAYING;
            }
        });
    }

    private void handleLeftClick(double x, double y) {
        if(state == GameState.MAIN_MENU){
            if (x >= 540 && x <= 740 && y >= 380 && y <= 430){
                state = GameState.PLAYING;
            }
        }
        // commit succ
    }

    public void update(double delta) {
        switch (state) {
            case PLAYING    -> updatePlaying(delta);
            case PAUSED     -> { /* nessun aggiornamento logico */ }
            case WAVE_INTRO -> { /* timer countdown */ }
            case GAME_OVER  -> { /* attendi input */ }
            case VICTORY    -> { /* attendi input */ }
            default         -> { /* MAIN_MENU: nessun update */ }
        }
    }
    private void updatePlaying(double delta) {
        //commit succ.
    }

/*
    public void render() {
         Pulisci schermo
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, Main.WIDTH, Main.HEIGHT);

        switch (state) {
            case MAIN_MENU  -> renderMainMenu();
            case PLAYING    -> renderPlaying();
            case PAUSED     -> renderPaused();
            case GAME_OVER  -> renderGameOver();
            case VICTORY    -> renderVictory();
            default         -> {}
        }
    } */
}
