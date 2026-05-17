package com.example.imperia;

public class Warrior {

    public static final int ARCHER = 0;
    public static final int MELEE  = 1;

    public final int col;
    public final int lane;
    public final int type;
    public final double cx;
    public final double cy;
    public int hp;
    public final int maxHp;
    public double atkTimer;
    public int animFrame;
    public double animTimer;
    public boolean shooting;

    public Warrior(int col, int lane, int type) {
        this.col = col;
        this.lane = lane;
        this.type = type;
        this.cx = col * GameManager.CELL_W + GameManager.CELL_W / 2.0;
        this.cy = GameManager.LANE_Y[lane];
        this.maxHp = type == ARCHER ? 80 : 160;
        this.hp = maxHp;
    }

    public int getCost() {
        return type == ARCHER ? 50 : 70;
    }

    public boolean isAlive() {
        return hp > 0;
    }
}
