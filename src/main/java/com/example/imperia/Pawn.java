package com.example.imperia;

public class Pawn {

    public double x;
    public double y;
    public double timer;
    public final int value;
    public boolean active = true;

    public Pawn(double x, double y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
        this.timer = 0.0;
    }
}
