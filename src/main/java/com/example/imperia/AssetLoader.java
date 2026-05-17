package com.example.imperia;

import javafx.scene.image.Image;

public class AssetLoader {

    public final Image imgGrass;
    public final Image imgCastle;
    public final Image imgTower;
    public final Image imgArchery;
    public final Image imgGold;
    public final Image imgArrow;
    public final Image pawnIdleGold;

    public final Image sheetWarriorBlueIdle;
    public final Image sheetWarriorBlueRun;
    public final Image sheetArcherBlueIdle;
    public final Image sheetArcherBlueShoot;
    public final Image sheetEnemyRun;
    public final Image sheetEnemyIdle;
    public final Image sheetEnemyAttack;

    public final Image fxExplosion1;
    public final Image fxExplosion2;

    private AssetLoader() {
        imgGrass            = load("/assets/terrain/grass.png");
        imgCastle           = load("/assets/buildings/blue/castle.png");
        imgTower            = load("/assets/buildings/blue/tower.png");
        imgArchery          = load("/assets/buildings/blue/archery.png");
        imgGold             = load("/assets/resources/gold.png");
        imgArrow            = load("/assets/units/blue/archer/arrow.png");
        pawnIdleGold        = load("/assets/units/blue/pawn/idle_gold.png");

        sheetWarriorBlueIdle= load("/assets/units/blue/warrior/idle.png");
        sheetWarriorBlueRun = load("/assets/units/blue/warrior/run.png");
        sheetArcherBlueIdle = load("/assets/units/blue/archer/idle.png");
        sheetArcherBlueShoot= load("/assets/units/blue/archer/shoot.png");
        sheetEnemyRun       = load("/assets/units/red/warrior/run.png");
        sheetEnemyIdle      = load("/assets/units/red/warrior/idle.png");
        sheetEnemyAttack    = load("/assets/units/red/warrior/attack1.png");

        fxExplosion1        = load("/assets/fx/explosion1.png");
        fxExplosion2        = load("/assets/fx/explosion2.png");
    }

    public static AssetLoader load() {
        AssetLoader assets = new AssetLoader();
        System.out.println("[Assets] castle=" + (assets.imgCastle != null)
                + " grass=" + (assets.imgGrass != null)
                + " warrior=" + (assets.sheetWarriorBlueIdle != null)
                + " enemy=" + (assets.sheetEnemyRun != null)
                + " pawnGold=" + (assets.pawnIdleGold != null)
                + " explosion=" + (assets.fxExplosion1 != null || assets.fxExplosion2 != null));
        return assets;
    }

    private Image load(String path) {
        try (var stream = getClass().getResourceAsStream(path)) {
            if (stream == null) {
                System.out.println("MISSING ASSET: " + path);
                return null;
            }
            Image image = new Image(stream);
            return image.isError() ? null : image;
        } catch (Exception ex) {
            System.out.println("ERROR LOADING: " + path + " -> " + ex.getMessage());
            return null;
        }
    }
}
