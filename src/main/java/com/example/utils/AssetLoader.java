package com.example.utils;


import javafx.scene.image.Image;


import java.util.HashMap;
import java.util.Map;


/**
 * Carica e cachea tutte le immagini PNG degli asset Tiny Swords.
 *
 * Uso:
 *   AssetLoader.init();                         // carica tutto una volta
 *   Image img = AssetLoader.get("grass");       // recupera dal cache
 *
 * Struttura cartelle attesa in src/main/resources/assets/:
 *   terrain/
 *     grass.png          → tile erba (Flat Ground)
 *     path.png           → tile sentiero
 *     water.png          → tile acqua
 *     elevated.png       → tile terreno rialzato
 *     stairs.png         → tile scale
 *   resources/
 *     gold.png           → deposito d'oro
 *     wood.png           → albero (legno)
 *   ui/
 *     livebar_bg.png     → sfondo barra vita (stretchable)
 *     livebar_fill.png   → riempimento barra vita
 *
 * Se un file non viene trovato, viene usata una texture di fallback colorata
 * (placeholder) così il gioco non crasha durante lo sviluppo.
 */
public class AssetLoader {


    private static final Map<String, Image> cache = new HashMap<>();
    private static boolean initialized = false;


    // Percorso base dentro resources/
    private static final String BASE = "/assets/";


    public static void init() {
        if (initialized) return;


        // Terrain
        load("grass",    "terrain/grass.png");
        load("path",     "terrain/path.png");
        load("water",    "terrain/water.png");
        load("elevated", "terrain/elevated.png");
        load("stairs",   "terrain/stairs.png");


        // Resources
        load("gold",     "resources/gold.png");
        load("wood",     "resources/wood.png");


        // UI
        load("livebar_bg",   "ui/livebar_bg.png");
        load("livebar_fill", "ui/livebar_fill.png");


        initialized = true;
        System.out.println("[AssetLoader] Caricati " + cache.size() + " asset.");
    }


    /**
     * Restituisce l'immagine associata alla chiave.
     * Se non trovata (asset non ancora inserito), restituisce null —
     * il renderer userà un rettangolo colorato come placeholder.
     */
    public static Image get(String key) {
        return cache.get(key);
    }


    public static boolean has(String key) {
        return cache.containsKey(key);
    }



    private static void load(String key, String path) {
        try {
            Image img = new Image(AssetLoader.class.getResourceAsStream(BASE + path));
            if (!img.isError()) {
                cache.put(key, img);
            } else {
                System.out.println("[AssetLoader] WARN: " + path + " non trovato, uso placeholder.");
            }
        } catch (Exception e) {
            System.out.println("[AssetLoader] WARN: " + path + " → " + e.getMessage());
        }
    }
}

