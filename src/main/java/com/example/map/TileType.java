package com.example.map;

/**
 * Tutti i tipi di tile possibili sulla mappa 64x64.
 *
 * Tiny Swords layer order (dal basso verso l'alto):
 *   BG_COLOR → WATER → WATER_FOAM → FLAT_GROUND → PATH → ELEVATED_GROUND → STAIRS
 *
 * Per il gioco:
 *   - GRASS        → edificabile (torri, edifici)
 *   - PATH         → percorso nemici (non edificabile)
 *   - WATER        → non attraversabile, non edificabile
 *   - ELEVATED     → decorativo / non edificabile
 *   - RESOURCE_GOLD / RESOURCE_WOOD → spawn risorse raccoglibili dai Pawn
 */
public enum TileType {

    // --- Terreno base ---
    GRASS,          // erba piatta — EDIFICABILE
    PATH,           // sentiero di terra — percorso nemici
    WATER,          // acqua — non attraversabile
    ELEVATED,       // terreno rialzato — decorativo
    STAIRS,         // scale tra livelli

    // --- Risorse (spawn casuale sulle GRASS tiles) ---
    RESOURCE_GOLD,  // deposito d'oro — Pawn lo raccoglie
    RESOURCE_WOOD,  // albero da tagliare — Pawn lo taglia

    // --- Speciali ---
    CASTLE,         // posizione del castello (angolo in basso a destra)
    SPAWN           // punto di apparizione nemici (angolo in alto a sinistra)
}
