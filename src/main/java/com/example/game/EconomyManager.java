package com.example.game;

/**
 * Gestisce le risorse economiche del giocatore: Oro e Legno.
 *
 * Oro  → serve per costruire edifici e torri
 * Legno → serve per riparare le difese danneggiate
 *
 * Chiamato dal GameManager ogni volta che:
 *  - un Pawn raccoglie una risorsa dalla mappa
 *  - il giocatore costruisce una torre (spende oro)
 *  - il giocatore ripara una difesa (spende legno)
 *  - un nemico viene sconfitto (guadagna oro)
 */
public class EconomyManager {

    private int gold;
    private int wood;

    // Valori iniziali
    private static final int START_GOLD = 150;
    private static final int START_WOOD = 50;

    // Quantità raccolta per ogni risorsa
    public static final int GOLD_PER_DEPOSIT = 25;
    public static final int WOOD_PER_TREE    = 20;

    // Drop nemici sconfitti
    public static final int GOLD_DROP_NORMAL = 10;
    public static final int GOLD_DROP_BOSS   = 100;

    public EconomyManager() {
        this.gold = START_GOLD;
        this.wood = START_WOOD;
    }

    // -------------------------------------------------------------------------
    // Guadagno
    // -------------------------------------------------------------------------

    public void addGold(int amount) { gold += amount; }
    public void addWood(int amount) { wood += amount; }

    /** Chiamato quando un Pawn raccoglie una risorsa.
     *  @param isGold true = oro, false = legno */
    public void collectResource(boolean isGold) {
        if (isGold) addGold(GOLD_PER_DEPOSIT);
        else        addWood(WOOD_PER_TREE);
    }

    /** Chiamato quando un nemico viene sconfitto */
    public void onEnemyDefeated(boolean isBoss) {
        addGold(isBoss ? GOLD_DROP_BOSS : GOLD_DROP_NORMAL);
    }

    // -------------------------------------------------------------------------
    // Spesa
    // -------------------------------------------------------------------------

    /** Tenta di spendere oro. Restituisce true se riuscito. */
    public boolean spendGold(int amount) {
        if (gold < amount) return false;
        gold -= amount;
        return true;
    }

    /** Tenta di spendere legno. Restituisce true se riuscito. */
    public boolean spendWood(int amount) {
        if (wood < amount) return false;
        wood -= amount;
        return true;
    }

    public boolean canAffordGold(int amount) { return gold >= amount; }
    public boolean canAffordWood(int amount) { return wood >= amount; }

    // -------------------------------------------------------------------------
    // Getters
    // -------------------------------------------------------------------------

    public int getGold() { return gold; }
    public int getWood() { return wood; }
}
