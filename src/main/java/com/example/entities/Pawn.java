package com.example.entities;

import com.example.map.Tile;
import com.example.map.TileMap;
import com.example.map.TileType;
import com.example.utils.Vec2;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Il Pawn — unità economica chiave.
 *
 * NON combatte. Il suo unico scopo è raccogliere risorse:
 *  1. Il giocatore clicca su un Pawn → il Pawn viene SELEZIONATO
 *  2. Il giocatore clicca su una tile RESOURCE_GOLD o RESOURCE_WOOD
 *  3. Il Pawn cammina verso la risorsa
 *  4. Quando arriva, raccoglie la risorsa e torna alla posizione iniziale
 *
 * Stati interni:
 *  IDLE       → fermo, aspetta ordini
 *  MOVING     → si sta muovendo verso il target
 *  COLLECTING → è arrivato, sta raccogliendo (piccolo delay)
 *  RETURNING  → torna alla posizione di spawn
 */
public class Pawn extends Entity {

    public enum PawnState { IDLE, MOVING, COLLECTING, RETURNING }

    private static final int    HP           = 50;
    private static final double SPEED        = 60.0;  // pixel al secondo
    private static final double COLLECT_TIME = 1.5;   // secondi per raccogliere
    private static final double ARRIVE_DIST  = 4.0;   // distanza per considerarsi "arrivato"
    private static final double RENDER_SIZE  = TileMap.RENDER_SIZE;

    private PawnState pawnState  = PawnState.IDLE;
    private boolean   selected   = false;

    private Vec2   targetPos;       // posizione in pixel della risorsa target
    private int    targetCol;       // colonna tile della risorsa
    private int    targetRow;       // riga tile della risorsa
    private Vec2   spawnPos;        // posizione iniziale (torna qui dopo)

    private double collectTimer = 0;
    private boolean lastResourceWasGold = false;

    // Callback per notificare il GameManager della risorsa raccolta
    private ResourceCallback callback;

    public interface ResourceCallback {
        void onResourceCollected(boolean isGold);
    }

    public Pawn(double x, double y, ResourceCallback callback) {
        super(x, y, HP);
        this.spawnPos = new Vec2(x, y);
        this.callback = callback;
    }

    // -------------------------------------------------------------------------
    // UPDATE
    // -------------------------------------------------------------------------

    @Override
    public void update(double delta) {
        switch (pawnState) {
            case MOVING     -> updateMoving(delta);
            case COLLECTING -> updateCollecting(delta);
            case RETURNING  -> updateReturning(delta);
            default         -> {}  // IDLE: niente da fare
        }
    }

    private void updateMoving(double delta) {
        Vec2 dir      = targetPos.sub(pos).normalize();
        Vec2 movement = dir.scale(SPEED * delta);
        pos = pos.add(movement);

        if (pos.distanceTo(targetPos) < ARRIVE_DIST) {
            pos           = targetPos;
            pawnState     = PawnState.COLLECTING;
            collectTimer  = 0;
        }
    }

    private void updateCollecting(double delta) {
        collectTimer += delta;
        if (collectTimer >= COLLECT_TIME) {
            // Notifica il GameManager
            if (callback != null) callback.onResourceCollected(lastResourceWasGold);
            pawnState = PawnState.RETURNING;
        }
    }

    private void updateReturning(double delta) {
        Vec2 dir      = spawnPos.sub(pos).normalize();
        Vec2 movement = dir.scale(SPEED * delta);
        pos = pos.add(movement);

        if (pos.distanceTo(spawnPos) < ARRIVE_DIST) {
            pos       = spawnPos;
            pawnState = PawnState.IDLE;
        }
    }

    // -------------------------------------------------------------------------
    // ORDINE DI RACCOLTA — chiamato dal GameManager al click
    // -------------------------------------------------------------------------

    /**
     * Assegna al Pawn una risorsa da raccogliere.
     * @param tile      la tile con RESOURCE_GOLD o RESOURCE_WOOD
     * @param offsetX   offset mappa per calcolare pixel
     * @param offsetY   offset mappa per calcolare pixel
     * @param isGold    true = oro, false = legno
     */
    public void assignResource(Tile tile, double offsetX, double offsetY, boolean isGold) {
        targetCol  = tile.getCol();
        targetRow  = tile.getRow();
        targetPos  = new Vec2(
            offsetX + targetCol * RENDER_SIZE + RENDER_SIZE / 2.0,
            offsetY + targetRow * RENDER_SIZE + RENDER_SIZE / 2.0
        );
        lastResourceWasGold = isGold;
        pawnState  = PawnState.MOVING;
        selected   = false;
    }

    // -------------------------------------------------------------------------
    // RENDER
    // -------------------------------------------------------------------------

    @Override
    public void render(GraphicsContext gc) {
        double size = RENDER_SIZE * 0.8;

        // Cerchio di selezione
        if (selected) {
            gc.setStroke(Color.web("#FFD700"));
            gc.setLineWidth(1.5);
            gc.strokeOval(pos.x - size / 2 - 2, pos.y - size / 2 - 2, size + 4, size + 4);
        }

        // Corpo del Pawn (placeholder: cerchio marrone)
        Color bodyColor = switch (pawnState) {
            case IDLE       -> Color.web("#8B6914");
            case MOVING     -> Color.web("#C9A84C");
            case COLLECTING -> Color.web("#FFD700");
            case RETURNING  -> Color.web("#A08030");
        };

        gc.setFill(bodyColor);
        gc.fillOval(pos.x - size / 2, pos.y - size / 2, size, size);

        // Simbolo risorsa durante la raccolta
        if (pawnState == PawnState.COLLECTING) {
            gc.setFill(Color.WHITE);
            gc.setFont(javafx.scene.text.Font.font(8));
            gc.setTextAlign(javafx.scene.text.TextAlignment.CENTER);
            gc.fillText(lastResourceWasGold ? "⛏" : "🪓", pos.x, pos.y - size);
        }

        // HP bar (solo se danneggiato)
        if (hp < maxHp) renderHpBar(gc, size, size / 2 + 4);
    }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public boolean    isSelected()  { return selected; }
    public PawnState  getPawnState(){ return pawnState; }
    public boolean    isIdle()      { return pawnState == PawnState.IDLE; }

    public void setSelected(boolean selected) { this.selected = selected; }

    /** Controlla se il click in pixel cade sopra questo Pawn */
    public boolean containsPoint(double px, double py) {
        double size = RENDER_SIZE * 0.8;
        return Math.abs(px - pos.x) < size / 2 && Math.abs(py - pos.y) < size / 2;
    }
}
