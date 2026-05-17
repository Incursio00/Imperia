package com.example.imperia;

import javafx.scene.image.Image;

public class Explosion {

    public final double x;
    public final double y;
    public final Image image;
    public double life = 0.45;

    public Explosion(double x, double y, Image image) {
        this.x = x;
        this.y = y;
        this.image = image;
    }
}
