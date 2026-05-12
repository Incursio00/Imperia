package com.example.imperia;

import com.example.entities.Enemy;
import com.example.entities.EnemyType;
import com.example.entities.Pawn;
import com.example.game.EconomyManager;
import com.example.game.WaveManager;
import com.example.map.Tile;
import com.example.map.TileMap;
import com.example.map.TileType;
import com.example.utils.AssetLoader;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import java.util.ArrayList;
import java.util.List;

public class GameManager {

    private final GraphicsContext gc;
    private final Scene           scene;

    private GameState state = GameState.MAIN_MENU;

    // Sotto-sistemi
    private TileMap         tileMap;
    private EconomyManager  economy;
    private WaveManager     waveManager;
    private List<Pawn>      pawns;

    // Castello
    private int    castleHp    = 100;
    private int    castleMaxHp = 100;

    // Selezione Pawn
    private Pawn selectedPawn = null;

    // Offset mappa
    private static final double MAP_OFFSET_X = 5;
    private static final double MAP_OFFSET_Y = 5;

    // Input
    private double mouseX, mouseY;

    public GameManager(GraphicsContext gc, Scene scene) {
        this.gc    = gc;
        this.scene = scene;
        AssetLoader.init();
        registerInput();
    }

    // -----------------------------------------------------------------------
    // INIT PARTITA
    // -----------------------------------------------------------------------

    private void initGame() {
        tileMap     = new TileMap();
        economy     = new EconomyManager();
        pawns       = new ArrayList<>();
        castleHp    = castleMaxHp;
        selectedPawn = null;

        // Spawna 3 Pawn vicino allo spawn point
        for (int i = 0; i < 3; i++) {
            double px = MAP_OFFSET_X + (5 + i * 3) * TileMap.RENDER_SIZE;
            double py = MAP_OFFSET_Y + 15 * TileMap.RENDER_SIZE;
            pawns.add(new Pawn(px, py, (isGold) -> economy.collectResource(isGold)));
        }

        waveManager = new WaveManager(tileMap, MAP_OFFSET_X, MAP_OFFSET_Y,
            new WaveManager.WaveCallback() {
                @Override public void onCastleHit(int damage) { damageCastle(damage); }
                @Override public void onEnemyDefeated(EnemyType type) { economy.onEnemyDefeated(type.isBoss()); }
                @Override public void onAllWavesDone() { state = GameState.VICTORY; }
            }
        );
    }

    private void damageCastle(int damage) {
        castleHp -= damage;
        if (castleHp <= 0) {
            castleHp = 0;
            state = GameState.GAME_OVER;
        }
    }

    // -----------------------------------------------------------------------
    // INPUT
    // -----------------------------------------------------------------------

    private void registerInput() {
        scene.setOnMouseMoved(e -> { mouseX = e.getX(); mouseY = e.getY(); });

        scene.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.PRIMARY)  handleLeftClick(e.getX(), e.getY());
            if (e.getButton() == MouseButton.SECONDARY) handleRightClick();
        });

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                if (state == GameState.PLAYING)        state = GameState.PAUSED;
                else if (state == GameState.PAUSED)    state = GameState.PLAYING;
            }
            if (e.getCode() == KeyCode.ENTER) {
                if (state == GameState.MAIN_MENU)      { initGame(); state = GameState.PLAYING; }
                if (state == GameState.GAME_OVER)      { initGame(); state = GameState.PLAYING; }
                if (state == GameState.VICTORY)        { initGame(); state = GameState.PLAYING; }
            }
        });
    }

    private void handleLeftClick(double x, double y) {
        if (state == GameState.MAIN_MENU) {
            if (x >= 540 && x <= 740 && y >= 380 && y <= 430) { initGame(); state = GameState.PLAYING; }
            return;
        }

        if (state != GameState.PLAYING) return;

        // 1. Clicca su un Pawn → selezionalo
        for (Pawn p : pawns) {
            if (p.containsPoint(x, y)) {
                deselectAll();
                p.setSelected(true);
                selectedPawn = p;
                return;
            }
        }

        // 2. Pawn selezionato + click su risorsa → assegna raccolta
        if (selectedPawn != null && selectedPawn.isIdle()) {
            int col = tileMap.pixelToCol(x, MAP_OFFSET_X);
            int row = tileMap.pixelToRow(y, MAP_OFFSET_Y);
            Tile tile = tileMap.getTile(col, row);

            if (tile != null) {
                TileType t = tile.getType();
                if (t == TileType.RESOURCE_GOLD || t == TileType.RESOURCE_WOOD) {
                    boolean isGold = (t == TileType.RESOURCE_GOLD);
                    tileMap.collectResource(col, row); // rimuove la risorsa dalla mappa
                    selectedPawn.assignResource(tile, MAP_OFFSET_X, MAP_OFFSET_Y, isGold);
                    selectedPawn = null;
                    return;
                }
            }
        }

        // 3. Click su area vuota → deseleziona
        deselectAll();
    }

    private void handleRightClick() {
        deselectAll();
    }

    private void deselectAll() {
        pawns.forEach(p -> p.setSelected(false));
        selectedPawn = null;
    }

    // -----------------------------------------------------------------------
    // UPDATE
    // -----------------------------------------------------------------------

    public void update(double delta) {
        if (state == GameState.PLAYING) {
            tileMap.update(delta);
            waveManager.update(delta);
            pawns.forEach(p -> p.update(delta));
        }
    }

    // -----------------------------------------------------------------------
    // RENDER
    // -----------------------------------------------------------------------

    public void render() {
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, Main.WIDTH, Main.HEIGHT);

        switch (state) {
            case MAIN_MENU -> renderMainMenu();
            case PLAYING   -> renderPlaying();
            case PAUSED    -> renderPaused();
            case GAME_OVER -> renderGameOver();
            case VICTORY   -> renderVictory();
            default        -> {}
        }
    }

    private void renderPlaying() {
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, Main.WIDTH - 280, Main.HEIGHT);

        tileMap.render(gc, MAP_OFFSET_X, MAP_OFFSET_Y);
        waveManager.render(gc);
        pawns.forEach(p -> p.render(gc));

        // HUD destro
        gc.setFill(Color.web("#12122a"));
        gc.fillRect(Main.WIDTH - 280, 0, 280, Main.HEIGHT);
        gc.setStroke(Color.web("#C9A84C66"));
        gc.setLineWidth(1);
        gc.strokeLine(Main.WIDTH - 280, 0, Main.WIDTH - 280, Main.HEIGHT);

        renderHUD();
    }

    private void renderHUD() {
        double hx = Main.WIDTH - 265;
        gc.setTextAlign(TextAlignment.LEFT);

        // Titolo
        gc.setFill(Color.web("#C9A84C"));
        gc.setFont(Font.font("Serif", 18));
        gc.fillText("⚔ IMPERIA", hx, 35);

        gc.setStroke(Color.web("#C9A84C44"));
        gc.setLineWidth(1);
        gc.strokeLine(Main.WIDTH - 275, 45, Main.WIDTH - 5, 45);

        // Ondata
        gc.setFill(Color.web("#aaaacc"));
        gc.setFont(Font.font("Serif", 13));
        gc.fillText("Ondata:  " + waveManager.getCurrentWave() + " / " + waveManager.getTotalWaves(), hx, 68);

        // Castello HP bar
        gc.setFill(Color.web("#C9A84C"));
        gc.setFont(Font.font("Serif", 13));
        gc.fillText("🏰 Castello", hx, 92);

        double barW = 240;
        double hpPct = (double) castleHp / castleMaxHp;
        gc.setFill(Color.web("#4a1a1a"));
        gc.fillRoundRect(hx, 98, barW, 12, 4, 4);
        Color hpColor = hpPct > 0.5 ? Color.web("#2ECC40") : hpPct > 0.25 ? Color.web("#FFDC00") : Color.web("#FF4136");
        gc.setFill(hpColor);
        gc.fillRoundRect(hx, 98, barW * hpPct, 12, 4, 4);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Sans Serif", 10));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(castleHp + " / " + castleMaxHp, hx + barW / 2, 108);
        gc.setTextAlign(TextAlignment.LEFT);

        // Risorse
        gc.setFill(Color.web("#C9A84C"));
        gc.setFont(Font.font("Serif", 13));
        gc.fillText("🪙 Oro:    " + economy.getGold(), hx, 135);
        gc.fillText("🪵 Legno:  " + economy.getWood(), hx, 155);

        // Pawn status
        gc.setStroke(Color.web("#C9A84C44"));
        gc.strokeLine(Main.WIDTH - 275, 170, Main.WIDTH - 5, 170);
        gc.setFill(Color.web("#C9A84C"));
        gc.setFont(Font.font("Serif", 13));
        gc.fillText("Pedoni:", hx, 190);

        for (int i = 0; i < pawns.size(); i++) {
            Pawn p = pawns.get(i);
            String status = switch (p.getPawnState()) {
                case IDLE       -> "inattivo";
                case MOVING     -> "in cammino";
                case COLLECTING -> "raccoglie";
                case RETURNING  -> "torna";
            };
            Color c = p.isSelected() ? Color.web("#FFD700") : Color.web("#aaaacc");
            gc.setFill(c);
            gc.setFont(Font.font("Sans Serif", 11));
            gc.fillText("  P" + (i + 1) + ": " + status, hx, 208 + i * 16);
        }

        // Istruzioni
        gc.setStroke(Color.web("#C9A84C44"));
        gc.strokeLine(Main.WIDTH - 275, 270, Main.WIDTH - 5, 270);
        gc.setFill(Color.web("#666688"));
        gc.setFont(Font.font("Sans Serif", 11));
        gc.fillText("Click pedone → seleziona", hx, 290);
        gc.fillText("Click risorsa → raccoglie", hx, 306);
        gc.fillText("ESC = pausa", hx, 322);
    }

    private void renderMainMenu() {
        gc.setFill(Color.web("#16213e"));
        gc.fillRect(0, 0, Main.WIDTH, Main.HEIGHT);

        gc.setFill(Color.web("#C9A84C"));
        gc.setFont(Font.font("Serif", 64));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("TOWER DEFENSE", Main.WIDTH / 2.0, 220);

        gc.setFill(Color.web("#E8C97A"));
        gc.setFont(Font.font("Serif", 40));
        gc.fillText("IMPERIA", Main.WIDTH / 2.0, 280);

        gc.setFill(Color.web("#C9A84C88"));
        gc.setFont(Font.font("Sans Serif", 15));
        gc.fillText("Il Regno Unito si Difende", Main.WIDTH / 2.0, 320);

        boolean hover = mouseX >= 540 && mouseX <= 740 && mouseY >= 380 && mouseY <= 430;
        gc.setFill(hover ? Color.web("#C9A84C") : Color.web("#8B6914"));
        gc.fillRoundRect(540, 380, 200, 50, 10, 10);
        gc.setFill(Color.web("#1a1a2e"));
        gc.setFont(Font.font("Serif", 20));
        gc.fillText("INIZIA PARTITA", Main.WIDTH / 2.0, 412);

        gc.setFill(Color.web("#C9A84C88"));
        gc.setFont(Font.font("Sans Serif", 13));
        gc.fillText("oppure premi INVIO", Main.WIDTH / 2.0, 460);

        gc.setFill(Color.web("#ffffff33"));
        gc.setFont(Font.font("Sans Serif", 11));
        gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("Assets: Tiny Swords — PixelFrog (itch.io)", Main.WIDTH - 16, Main.HEIGHT - 12);
        gc.setTextAlign(TextAlignment.CENTER);
    }

    private void renderPaused() {
        renderPlaying();
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
        gc.setFill(Color.web("#1a0404"));
        gc.fillRect(0, 0, Main.WIDTH, Main.HEIGHT);
        gc.setFill(Color.web("#FF4136"));
        gc.setFont(Font.font("Serif", 52));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("IL CASTELLO È CADUTO", Main.WIDTH / 2.0, Main.HEIGHT / 2.0 - 30);
        gc.setFill(Color.web("#ffaaaa"));
        gc.setFont(Font.font("Sans Serif", 18));
        gc.fillText("Imperia è perduta.", Main.WIDTH / 2.0, Main.HEIGHT / 2.0 + 20);
        gc.setFill(Color.web("#ffffff55"));
        gc.setFont(Font.font("Sans Serif", 14));
        gc.fillText("Premi INVIO per riprovare", Main.WIDTH / 2.0, Main.HEIGHT / 2.0 + 60);
    }

    private void renderVictory() {
        gc.setFill(Color.web("#0a1a0a"));
        gc.fillRect(0, 0, Main.WIDTH, Main.HEIGHT);
        gc.setFill(Color.web("#C9A84C"));
        gc.setFont(Font.font("Serif", 52));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText("IMPERIA È SALVA!", Main.WIDTH / 2.0, Main.HEIGHT / 2.0 - 30);
        gc.setFill(Color.web("#C9A84Caa"));
        gc.setFont(Font.font("Sans Serif", 18));
        gc.fillText("Il Gran Comandante ha resistito a tutte le ondate.", Main.WIDTH / 2.0, Main.HEIGHT / 2.0 + 20);
        gc.setFill(Color.web("#ffffff55"));
        gc.setFont(Font.font("Sans Serif", 14));
        gc.fillText("Premi INVIO per giocare di nuovo", Main.WIDTH / 2.0, Main.HEIGHT / 2.0 + 60);
    }
}
