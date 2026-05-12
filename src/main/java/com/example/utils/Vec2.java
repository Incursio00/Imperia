package com.example.utils;


/**
 * Vettore 2D immutabile — usato per posizioni, velocità, direzioni.
 * Operazioni restituiscono sempre un nuovo Vec2 (immutabilità).
 */
public class Vec2 {

    public final double x;
    public final double y;

    public static final Vec2 ZERO = new Vec2(0, 0);

    public Vec2(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Vec2 add(Vec2 other)          { return new Vec2(x + other.x, y + other.y); }
    public Vec2 sub(Vec2 other)          { return new Vec2(x - other.x, y - other.y); }
    public Vec2 scale(double factor)     { return new Vec2(x * factor,  y * factor);  }

    public double length() {
        return Math.sqrt(x * x + y * y);
    }

    /** Vettore normalizzato (lunghezza 1). Restituisce ZERO se lunghezza è 0. */
    public Vec2 normalize() {
        double len = length();
        if (len == 0) return ZERO;
        return new Vec2(x / len, y / len);
    }

    /** Distanza euclidea verso un altro punto */
    public double distanceTo(Vec2 other) {
        return sub(other).length();
    }

    @Override
    public String toString() { return "Vec2(" + x + ", " + y + ")"; }
}
