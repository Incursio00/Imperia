package com.example.imperia;

import java.util.Random;

import javafx.scene.image.Image;

public class Enemy {

    private static final Random RNG = new Random();

    public double x;
    public final double y;
    public final int lane;
    public int hp;
    public final int maxHp;
    public final double speed;
    public boolean attacking;
    public double atkTimer;
    public int animFrame;
    public double animTimer;
    public boolean dead;
    public double deadTimer;
    public final int reward;

    public Enemy(int lane, double startX, double startY) {
        this.lane = lane;
        this.x = startX;
        this.y = startY;
        this.maxHp = 60 + RNG.nextInt(40);
        this.hp = maxHp;
        this.speed = 40 + RNG.nextInt(20);
        this.reward = 8;
    }

    public boolean isAlive() {
        return !dead && hp > 0;
    }

    public int getFrameCount() {
        return attacking ? 4 : 6;
    }

    public Image getSpriteSheet(AssetLoader assets) {
        if (dead) return assets.sheetEnemyIdle;
        if (attacking) return assets.sheetEnemyAttack;
        return assets.sheetEnemyRun;
    }
}
