package com.example.map;

import com.example.utils.AssetLoader;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * La mappa di gioco — griglia 64x64 di Tile.
 *
 * Responsabilità:
 *  1. Costruisce la griglia con il layout della mappa
 *  2. Definisce il percorso dei nemici come lista di waypoint
 *  3. Gestisce lo spawn casuale delle risorse (oro e legno)
 *  4. Renderizza tutti i tile con gli asset Tiny Swords (o placeholder)
 *
 * Coordinate: col = X (sinistra→destra), row = Y (alto→basso)
 * Tile SIZE: 64px — la mappa totale occupa 64*64 = 4096x4096 px
 *
 * Per la finestra 1280x720 usiamo una CAMERA con offset e zoom.
 * In questo commit la mappa è renderizzata a zoom ridotto (16px/tile)
 * per vedere l'intera mappa nell'area di gioco (1000x720 px).
 */
public class TileMap {

    public static final int COLS = 64;
    public static final int ROWS = 64;

    // Tile renderizzati a dimensione ridotta per mostrare l'intera mappa
    // 1000px / 64 tile ≈ 15px per tile (usiamo 15 per stare nell'area HUD)
    public static final int RENDER_SIZE = 15;

    private final Tile[][]       grid;
    private final List<int[]>    waypoints;   // {col, row} — percorso nemici
    private final List<int[]>    resourceSpawnTiles; // tile GRASS eleggibili per risorse
    private final Random         rng = new Random();

    // Timer per spawn risorse
    private double resourceTimer    = 0;
    private static final double RESOURCE_INTERVAL = 8.0; // secondi tra uno spawn e l'altro
    private static final int    MAX_RESOURCES      = 6;  // max risorse contemporanee sulla mappa

    public TileMap() {
        grid      = new Tile[COLS][ROWS];
        waypoints = new ArrayList<>();
        resourceSpawnTiles = new ArrayList<>();
        buildMap();
        buildWaypoints();
        collectGrassTiles();
    }

    // =========================================================================
    // COSTRUZIONE MAPPA
    // =========================================================================

    /**
     * Costruisce il layout della mappa.
     *
     * Layout (semplificato per il primo commit della mappa):
     *
     *  S = SPAWN  (colonna 0, riga 8–10)
     *  P = PATH   (percorso a forma di Z attraverso la mappa)
     *  C = CASTLE (angolo in basso a destra)
     *  ~ = WATER  (bordo sinistro e inferiore)
     *  G = GRASS  (tutto il resto — edificabile)
     *
     * Il percorso entra da sinistra, scende, attraversa al centro, risale,
     * poi scende di nuovo verso il castello in basso a destra.
     */
    private void buildMap() {
        // 1. Riempie tutto con GRASS
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                grid[c][r] = new Tile(c, r, TileType.GRASS);
            }
        }

        // 2. Bordo acqua (colonna 0 e riga 63)
        for (int r = 0; r < ROWS; r++) setTile(0, r, TileType.WATER);
        for (int c = 0; c < COLS; c++) setTile(c, 63, TileType.WATER);

        // 3. Terreno rialzato (decorativo) — striscia in alto
        for (int c = 0; c < COLS; c++) {
            setTile(c, 0,  TileType.ELEVATED);
            setTile(c, 1,  TileType.ELEVATED);
        }

        // 4. Percorso nemici a forma di Z
        //    Segmento A: entrata da sinistra → orizzontale riga 10
        for (int c = 1; c <= 20; c++)  setTile(c, 10, TileType.PATH);
        for (int c = 1; c <= 20; c++)  setTile(c, 11, TileType.PATH);

        //    Segmento B: scende verso il centro (col 20, righe 10→32)
        for (int r = 10; r <= 32; r++) setTile(20, r, TileType.PATH);
        for (int r = 10; r <= 32; r++) setTile(21, r, TileType.PATH);

        //    Segmento C: attraversa al centro (col 20→43, riga 32)
        for (int c = 20; c <= 43; c++) setTile(c, 32, TileType.PATH);
        for (int c = 20; c <= 43; c++) setTile(c, 33, TileType.PATH);

        //    Segmento D: risale (col 43, righe 32→12)
        for (int r = 12; r <= 33; r++) setTile(43, r, TileType.PATH);
        for (int r = 12; r <= 33; r++) setTile(44, r, TileType.PATH);

        //    Segmento E: orizzontale in alto destra (col 43→58, riga 12)
        for (int c = 43; c <= 58; c++) setTile(c, 12, TileType.PATH);
        for (int c = 43; c <= 58; c++) setTile(c, 13, TileType.PATH);

        //    Segmento F: scende verso il castello (col 58, righe 12→55)
        for (int r = 12; r <= 55; r++) setTile(58, r, TileType.PATH);
        for (int r = 12; r <= 55; r++) setTile(59, r, TileType.PATH);

        //    Segmento G: orizzontale finale verso castello
        for (int c = 55; c <= 62; c++) setTile(c, 55, TileType.PATH);
        for (int c = 55; c <= 62; c++) setTile(c, 56, TileType.PATH);

        // 5. Spawn point (angolo sinistro riga 10)
        setTile(1, 10, TileType.SPAWN);
        setTile(1, 11, TileType.SPAWN);

        // 6. Castello (angolo in basso a destra)
        for (int c = 59; c <= 62; c++)
            for (int r = 55; r <= 61; r++)
                setTile(c, r, TileType.CASTLE);
    }

    private void setTile(int col, int row, TileType type) {
        if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
            grid[col][row].setType(type);
        }
    }

    // =========================================================================
    // WAYPOINT PATH (centro del percorso, 1 tile di larghezza logica)
    // =========================================================================

    /**
     * Definisce i waypoint che le unità nemiche seguiranno.
     * Ogni waypoint è {col, row} del centro del percorso.
     * Il PathFollower dei nemici interpola tra un waypoint e il successivo.
     */
    private void buildWaypoints() {
        // Entrata
        addWP(1,  10);
        // Fine segmento A
        addWP(20, 10);
        // Fine segmento B (basso)
        addWP(20, 32);
        // Fine segmento C (centro)
        addWP(43, 32);
        // Fine segmento D (risalita)
        addWP(43, 12);
        // Fine segmento E (destra)
        addWP(58, 12);
        // Fine segmento F (discesa)
        addWP(58, 55);
        // Castello
        addWP(62, 55);
    }

    private void addWP(int col, int row) {
        waypoints.add(new int[]{col, row});
    }

    // =========================================================================
    // RISORSE
    // =========================================================================

    /** Raccoglie tutte le tile GRASS lontane dal percorso per lo spawn risorse */
    private void collectGrassTiles() {
        for (int c = 2; c < COLS - 2; c++) {
            for (int r = 2; r < ROWS - 2; r++) {
                if (grid[c][r].getType() == TileType.GRASS) {
                    resourceSpawnTiles.add(new int[]{c, r});
                }
            }
        }
    }

    /**
     * Aggiornamento chiamato ogni frame.
     * Gestisce il timer di spawn risorse.
     */
    public void update(double delta) {
        resourceTimer += delta;
        if (resourceTimer >= RESOURCE_INTERVAL) {
            resourceTimer = 0;
            trySpawnResource();
        }
    }

    /**
     * Prova a far apparire una risorsa casuale su una GRASS tile libera.
     * Alterna tra RESOURCE_GOLD e RESOURCE_WOOD.
     */
    private void trySpawnResource() {
        // Conta risorse già presenti
        long current = countResourceTiles();
        if (current >= MAX_RESOURCES) return;

        // Scegli una tile GRASS libera a caso
        List<int[]> candidates = new ArrayList<>();
        for (int[] pos : resourceSpawnTiles) {
            Tile t = grid[pos[0]][pos[1]];
            if (t.getType() == TileType.GRASS && !t.isOccupied()) {
                candidates.add(pos);
            }
        }
        if (candidates.isEmpty()) return;

        int[] chosen = candidates.get(rng.nextInt(candidates.size()));
        TileType resource = (rng.nextBoolean()) ? TileType.RESOURCE_GOLD : TileType.RESOURCE_WOOD;
        setTile(chosen[0], chosen[1], resource);

        System.out.println("[TileMap] Risorsa " + resource + " spawned @ " + chosen[0] + "," + chosen[1]);
    }

    /** Raccoglie una risorsa (chiamato dal Pawn) */
    public boolean collectResource(int col, int row) {
        Tile t = getTile(col, row);
        if (t == null) return false;
        if (t.getType() != TileType.RESOURCE_GOLD && t.getType() != TileType.RESOURCE_WOOD) return false;

        boolean isGold = t.getType() == TileType.RESOURCE_GOLD;
        t.setType(TileType.GRASS); // rimuove la risorsa
        return isGold; // true = oro, false = legno
    }

    private long countResourceTiles() {
        long count = 0;
        for (int c = 0; c < COLS; c++)
            for (int r = 0; r < ROWS; r++) {
                TileType type = grid[c][r].getType();
                if (type == TileType.RESOURCE_GOLD || type == TileType.RESOURCE_WOOD) count++;
            }
        return count;
    }

    // =========================================================================
    // RENDERING
    // =========================================================================

    /**
     * Renderizza l'intera mappa nell'area di gioco.
     * Usa gli asset Tiny Swords se disponibili, altrimenti rettangoli colorati.
     *
     * @param gc     GraphicsContext del canvas
     * @param offsetX spostamento orizzontale in pixel (per camera/scroll futuro)
     * @param offsetY spostamento verticale in pixel
     */
    public void render(GraphicsContext gc, double offsetX, double offsetY) {
        for (int c = 0; c < COLS; c++) {
            for (int r = 0; r < ROWS; r++) {
                Tile t = grid[c][r];
                double px = offsetX + c * RENDER_SIZE;
                double py = offsetY + r * RENDER_SIZE;

                renderTile(gc, t, px, py);
            }
        }

        // Sovrappone i waypoint in debug (rimovibile)
        renderWaypointsDebug(gc, offsetX, offsetY);
    }

    private void renderTile(GraphicsContext gc, Tile t, double px, double py) {
        String assetKey = tileToAssetKey(t.getType());
        Image  img      = AssetLoader.get(assetKey);

        if (img != null) {
            // Usa l'asset Tiny Swords
            gc.drawImage(img, px, py, RENDER_SIZE, RENDER_SIZE);
        } else {
            // Placeholder colorato — usato finché non inserisci gli asset
            gc.setFill(tileToColor(t.getType()));
            gc.fillRect(px, py, RENDER_SIZE, RENDER_SIZE);

            // Bordo sottile per distinguere le tile
            gc.setStroke(Color.web("#00000022"));
            gc.setLineWidth(0.3);
            gc.strokeRect(px, py, RENDER_SIZE, RENDER_SIZE);
        }
    }

    /** Mostra i waypoint come cerchi rossi (debug) */
    private void renderWaypointsDebug(GraphicsContext gc, double offsetX, double offsetY) {
        gc.setFill(Color.web("#FF000088"));
        for (int[] wp : waypoints) {
            double px = offsetX + wp[0] * RENDER_SIZE + RENDER_SIZE / 2.0;
            double py = offsetY + wp[1] * RENDER_SIZE + RENDER_SIZE / 2.0;
            gc.fillOval(px - 3, py - 3, 6, 6);
        }
    }

    // =========================================================================
    // UTILITY
    // =========================================================================

    private String tileToAssetKey(TileType type) {
        return switch (type) {
            case GRASS          -> "grass";
            case PATH           -> "path";
            case WATER          -> "water";
            case ELEVATED       -> "elevated";
            case STAIRS         -> "stairs";
            case RESOURCE_GOLD  -> "gold";
            case RESOURCE_WOOD  -> "wood";
            case CASTLE         -> "grass"; // il castello è un'entità sopra la tile, non la tile stessa
            case SPAWN          -> "path";
        };
    }

    private Color tileToColor(TileType type) {
        return switch (type) {
            case GRASS          -> Color.web("#4a7c3f");
            case PATH           -> Color.web("#c8a96a");
            case WATER          -> Color.web("#2a6e9e");
            case ELEVATED       -> Color.web("#5a8a50");
            case STAIRS         -> Color.web("#a89060");
            case RESOURCE_GOLD  -> Color.web("#FFD700");
            case RESOURCE_WOOD  -> Color.web("#8B4513");
            case CASTLE         -> Color.web("#8888cc");
            case SPAWN          -> Color.web("#cc4444");
        };
    }

    public Tile getTile(int col, int row) {
        if (col < 0 || col >= COLS || row < 0 || row >= ROWS) return null;
        return grid[col][row];
    }

    /** Converte pixel → colonna griglia */
    public int pixelToCol(double px, double offsetX) {
        return (int) ((px - offsetX) / RENDER_SIZE);
    }

    /** Converte pixel → riga griglia */
    public int pixelToRow(double py, double offsetY) {
        return (int) ((py - offsetY) / RENDER_SIZE);
    }

    public List<int[]> getWaypoints() { return waypoints; }
}
