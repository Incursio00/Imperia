package com.example.entities;

import com.example.utils.Vec2;
import javafx.scene.canvas.GraphicsContext;

/**
 * Classe base per tutte le entità di gioco (Pawn, nemici, unità alleate).
 *
 * Ogni entità ha:
 *  - posizione in pixel (Vec2 pos)
 *  - HP correnti e massimi
 *  - flag alive
 *  - metodi update() e render() da sovrascrivere
 */
public abstract class Entity {

    protected Vec2    pos;
    protected int     hp;
    protected int     maxHp;
    protected boolean alive;

    public Entity(double x, double y, int maxHp) {
        this.pos   = new Vec2(x, y);
        this.maxHp = maxHp;
        this.hp    = maxHp;
        this.alive = true;
    }

    // -------------------------------------------------------------------------
    // Update / Render — da implementare nelle sottoclassi
    // -------------------------------------------------------------------------

    public abstract void update(double delta);
    public abstract void render(GraphicsContext gc);

    // -------------------------------------------------------------------------
    // Danno / Salute
    // -------------------------------------------------------------------------

    public void takeDamage(int amount) {
        hp -= amount;
        if (hp <= 0) {
            hp    = 0;
            alive = false;
        }
    }

    public void heal(int amount) {
        hp = Math.min(hp + amount, maxHp);
    }

    /** Percentuale HP 0.0 → 1.0 */
    public double getHpPercent() {
        return (double) hp / maxHp;
    }

    // -------------------------------------------------------------------------
    // Utility rendering HP bar
    // -------------------------------------------------------------------------

    protected void renderHpBar(GraphicsContext gc, double width, double yOffset) {
        double barX = pos.x - width / 2;
        double barY = pos.y - yOffset;

        // Sfondo rosso
        gc.setFill(javafx.scene.paint.Color.web("#8B1A1A"));
        gc.fillRect(barX, barY, width, 4);

        // Riempimento verde
        gc.setFill(javafx.scene.paint.Color.web("#2ECC40"));
        gc.fillRect(barX, barY, width * getHpPercent(), 4);
    }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public Vec2    getPos()    { return pos; }
    public int     getHp()     { return hp; }
    public int     getMaxHp()  { return maxHp; }
    public boolean isAlive()   { return alive; }

    public double  getX()      { return pos.x; }
    public double  getY()      { return pos.y; }
}
