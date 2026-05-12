package com.example.entities;

/**
 * Tutti i tipi di nemico con le loro statistiche base.
 *
 * hp     → punti vita
 * speed  → pixel al secondo lungo il percorso
 * damage → danno inflitto al castello quando lo raggiunge
 * reward → oro rilasciato alla sconfitta
 */
public enum EnemyType {

    //                    hp    speed  damage  reward
    GNOME       (  60,   90,     10,     8),   // veloce, ignora unità
    THIEF       (  50,   95,     10,     8),   // veloce, ignora unità
    TURTLE      ( 200,   25,     20,    15),   // lento, corazzato
    LIZARD      ( 180,   28,     20,    15),   // lento, branchie spinose
    SHAMAN      (  80,   40,     15,    20),   // attacca torri da lontano
    GNOLL       (  90,   42,     15,    20),   // attacca torri da lontano
    ORC_BOSS    (1500,   20,    100,   100);   // boss finale

    public final int    hp;
    public final double speed;
    public final int    damage;
    public final int    reward;

    EnemyType(int hp, double speed, int damage, int reward) {
        this.hp     = hp;
        this.speed  = speed;
        this.damage = damage;
        this.reward = reward;
    }

    public boolean isRunner()     { return this == GNOME || this == THIEF; }
    public boolean isArmored()    { return this == TURTLE || this == LIZARD; }
    public boolean isRanged()     { return this == SHAMAN || this == GNOLL; }
    public boolean isBoss()       { return this == ORC_BOSS; }
    public boolean hasSpines()    { return this == LIZARD; } // danneggia chi la tocca
}
