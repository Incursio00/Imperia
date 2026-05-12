package com.example.map;

/**
 * Rappresenta una singola cella della TileMap.
 *
 * Ogni Tile sa:
 *  - la sua posizione nella griglia (col, row)
 *  - il suo tipo (TileType)
 *  - se è occupata da un edificio o unità
 *  - se contiene una risorsa raccoglibile
 */
public class Tile {

    // Dimensione in pixel di ogni tile (Tiny Swords usa 64x64)
    public static final int SIZE = 64;

    private final int col;       // colonna nella griglia (0..63)
    private final int row;       // riga nella griglia (0..63)
    private TileType  type;
    private boolean   occupied;  // true se c'è una torre/edificio sopra
    private boolean   hasResource; // true se c'è una risorsa raccoglibile

    public Tile(int col, int row, TileType type) {
        this.col  = col;
        this.row  = row;
        this.type = type;
        this.occupied    = false;
        this.hasResource = (type == TileType.RESOURCE_GOLD || type == TileType.RESOURCE_WOOD);
    }

    // -------------------------------------------------------------------------
    // Query utili
    // -------------------------------------------------------------------------

    /** Può essere costruita una torre su questa tile? */
    public boolean isBuildable() {
        return type == TileType.GRASS && !occupied;
    }

    /** I nemici possono camminare su questa tile? */
    public boolean isWalkable() {
        return type == TileType.PATH || type == TileType.SPAWN;
    }

    /** Posizione in pixel del bordo superiore sinistro della tile */
    public double getPixelX() { return col * SIZE; }
    public double getPixelY() { return row * SIZE; }

    /** Centro della tile in pixel */
    public double getCenterX() { return col * SIZE + SIZE / 2.0; }
    public double getCenterY() { return row * SIZE + SIZE / 2.0; }

    // -------------------------------------------------------------------------
    // Getters / Setters
    // -------------------------------------------------------------------------

    public int      getCol()         { return col; }
    public int      getRow()         { return row; }
    public TileType getType()        { return type; }
    public boolean  isOccupied()     { return occupied; }
    public boolean  hasResource()    { return hasResource; }

    public void setType(TileType type) {
        this.type = type;
        this.hasResource = (type == TileType.RESOURCE_GOLD || type == TileType.RESOURCE_WOOD);
    }

    public void setOccupied(boolean occupied)       { this.occupied = occupied; }
    public void setHasResource(boolean hasResource) { this.hasResource = hasResource; }

    @Override
    public String toString() {
        return "Tile[" + col + "," + row + " " + type + "]";
    }
}
