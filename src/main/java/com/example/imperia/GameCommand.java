package com.example.imperia;

import com.example.map.TileMap;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

public class GameCommand {

    private final GraphicsContext gc;
    private final Scene scene;
    private static final double MAP_OFFSET_X = 5;
    private static final double MAP_OFFSET_Y = 5;
    private TileMap tileMap;

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

    public void render() {
        // Pulisci schermo
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
    }

    private void renderMainMenu() {
        // Sfondo decorativo: griglia di tile placeholder
        gc.setFill(Color.web("#16213e"));
        gc.fillRect(0, 0, Main.WIDTH, Main.HEIGHT);

        // Titolo
        gc.setFill(Color.web("#C9A84C"));
        gc.setFont(Font.font("Serif", 64));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("TOWER DEFENSE", Main.WIDTH / 2.0, 220);

        gc.setFill(Color.web("#E8C97A"));
        gc.setFont(Font.font("Serif", 40));
        gc.fillText("IMPERIA", Main.WIDTH / 2.0, 280);

        gc.setFill(Color.web("#C9A84C55"));
        gc.setFont(Font.font("Sans Serif", 16));
        gc.fillText("Il Regno Unito si Difende", Main.WIDTH / 2.0, 320);

        // Bottone Inizia
        boolean hover = mouseX >= 540 && mouseX <= 740 && mouseY >= 380 && mouseY <= 430;
        gc.setFill(hover ? Color.web("#C9A84C") : Color.web("#8B6914"));
        gc.fillRoundRect(540, 380, 200, 50, 10, 10);
        gc.setFill(Color.web("#1a1a2e"));
        gc.setFont(Font.font("Serif", 20));
        gc.fillText("INIZIA PARTITA", Main.WIDTH / 2.0, 412);

        // Suggerimento tastiera
        gc.setFill(Color.web("#C9A84C88"));
        gc.setFont(Font.font("Sans Serif", 13));
        gc.fillText("oppure premi INVIO", Main.WIDTH / 2.0, 460);

        // Crediti asset
        gc.setFill(Color.web("#ffffff33"));
        gc.setFont(Font.font("Sans Serif", 11));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("Assets: Tiny Swords — PixelFrog (itch.io)", Main.WIDTH - 16, Main.HEIGHT - 12);
        gc.setTextAlign(TextAlignment.CENTER);
    }

    private void renderPlaying() {
        // Area di gioco (sfondo)
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, Main.WIDTH - 280, Main.HEIGHT);

        // Renderizza la TileMap
        tileMap.render(gc, MAP_OFFSET_X, MAP_OFFSET_Y);

        // Pannello HUD destro
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(Main.WIDTH - 280, 0, 280, Main.HEIGHT);
        gc.setStroke(Color.web("#C9A84C55"));
        gc.setLineWidth(1);
        gc.strokeLine(Main.WIDTH - 280, 0, Main.WIDTH - 280, Main.HEIGHT);

        renderHUDPlaceholder();
    }

    private void renderHUDPlaceholder() {
        double hx = Main.WIDTH - 270;

        gc.setFill(Color.web("#C9A84C"));
        gc.setFont(Font.font("Serif", 16));
        gc.setTextAlign(TextAlignment.LEFT);
        gc.fillText("IMPERIA — HUD", hx, 40);

        gc.setFill(Color.web("#ffffff55"));
        gc.setFont(Font.font("Sans Serif", 12));
        gc.fillText("🪙 Oro:   100", hx, 80);
        gc.fillText("🪵 Legno:  50", hx, 100);
        gc.fillText("❤️  Castello: 100%", hx, 120);
        gc.fillText("🌊 Ondata:  1 / ?", hx, 140);
    }

    private void renderPaused() {
        renderPlaying(); // mappa sotto

        // Overlay semitrasparente
        gc.setFill(Color.web("#00000099"));
        gc.fillRect(0, 0, Main.WIDTH, Main.HEIGHT);

        gc.setFill(Color.web("#C9A84C"));
        gc.setFont(Font.font("Serif", 48));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("PAUSA", Main.WIDTH / 2.0, Main.HEIGHT / 2.0 - 20);

        gc.setFill(Color.web("#ffffff99"));
        gc.setFont(Font.font("Sans Serif", 16));
        gc.fillText("ESC per riprendere", Main.WIDTH / 2.0, Main.HEIGHT / 2.0 + 30);
    }

    private void renderGameOver() {
        gc.setFill(Color.web("#8B1A1A"));
        gc.fillRect(0, 0, Main.WIDTH, Main.HEIGHT);

        gc.setFill(Color.web("#ffdddd"));
        gc.setFont(Font.font("Serif", 56));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("IL CASTELLO È CADUTO", Main.WIDTH / 2.0, Main.HEIGHT / 2.0);

        gc.setFill(Color.web("#ffddddaa"));
        gc.setFont(Font.font("Sans Serif", 18));
        gc.fillText("Imperia è perduta.", Main.WIDTH / 2.0, Main.HEIGHT / 2.0 + 50);
    }

    private void renderVictory() {
        gc.setFill(Color.web("#1A3A1A"));
        gc.fillRect(0, 0, Main.WIDTH, Main.HEIGHT);

        gc.setFill(Color.web("#C9A84C"));
        gc.setFont(Font.font("Serif", 56));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("IMPERIA È SALVA!", Main.WIDTH / 2.0, Main.HEIGHT / 2.0);

        gc.setFill(Color.web("#C9A84Caa"));
        gc.setFont(Font.font("Sans Serif", 18));
        gc.fillText("Il Gran Comandante ha resistito a tutte le ondate.", Main.WIDTH / 2.0, Main.HEIGHT / 2.0 + 50);
    }
}
