package com.example.imperia;

/**
 * Stati possibili della macchina a stati del gioco.
 */
public enum GameState {
    MAIN_MENU,   // schermata iniziale
    PLAYING,     // partita in corso
    PAUSED,      // pausa
    WAVE_INTRO,  // breve schermata "Ondata N in arrivo"
    GAME_OVER,   // castello caduto
    VICTORY      // tutte le ondate completate
}
