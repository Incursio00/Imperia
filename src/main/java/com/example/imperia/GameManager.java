package com.example.imperia;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

// GameManager gestisce partita, update, render e input.
public class GameManager {

    // COSTANTI LAYOUT
    static final int GAME_W   = 960;
    static final int LANES    = 5;
    static final int COLS     = 8;
    static final int CELL_W   = GAME_W / COLS;   // 120px
    static final int CELL_H   = Main.HEIGHT / LANES; // 144px
    static final int SPRITE   = 96; // dimensione disegno sprite in px

    // Centro Y di ogni corsia
    static final double[] LANE_Y = new double[LANES];
    static { for (int i = 0; i < LANES; i++) LANE_Y[i] = (i + 0.5) * CELL_H; }

    private final AssetLoader assets;
    private GameState state = GameState.MAIN_MENU;
    private final Warrior[][] grid = new Warrior[COLS][LANES];
    private final List<Enemy>  enemies    = new ArrayList<>();
    private final List<Bullet> bullets    = new ArrayList<>();
    private final List<Gold>   coins      = new ArrayList<>();
    private final List<Pawn>   pawns      = new ArrayList<>();
    private final List<Explosion> explosions = new ArrayList<>();
    private int gold       = 150;
    private int castleHp   = 20;
    private static final int CASTLE_MAX = 20;
    private int    wave        = 0;
    private double pauseTimer  = 4.0;
    private boolean waveActive = false;
    private int    spawned     = 0;
    private double spawnTimer  = 0;
    private static final int TOTAL_WAVES = 5;
    private int selectedTower = 0;
    private double animTimer = 0;
    private int    animFrame = 0; 
    private double mouseX, mouseY;
    private final GraphicsContext gc;
    private final Scene scene;
    private final Canvas canvas;
    private final Random rng = new Random();

    // COSTRUTTORE
    
    public GameManager(GraphicsContext gc, Scene scene, Canvas canvas) {
        this.gc = gc;
        this.scene = scene;
        this.canvas = canvas;
        this.assets = AssetLoader.load();
        registerInput();
    }

    private static class Bullet {
        double x, y;
        int targetLane;
        double speed = 350;
        int damage = 15;
        Bullet(double sx, double sy, int lane) {
            this.x = sx;
            this.y = sy;
            this.targetLane = lane;
        }
    }

    private static class Gold {
        double x, y, vy;
        int value;
        double life = 4.0;
        Gold(double x, double y, int v) { this.x=x; this.y=y; this.vy=-30; this.value=v; }
    }

    // INPUT
    private void registerInput() {
        canvas.setOnMouseMoved(e -> { mouseX = e.getX(); mouseY = e.getY(); });

        canvas.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            double mx = e.getX(), my = e.getY();

            if (state == GameState.MAIN_MENU) { if (inRect(mx, my, 490, 340, 300, 60)) startGame(); return; }
            if (state == GameState.GAME_OVER || state == GameState.VICTORY) { startGame(); return; }
            if (state != GameState.PLAYING) return;

            // Raccolta monete d'oro (solo se la pawn non ha già raccolto)
            for (Gold g : coins) {
                if (Math.hypot(mx - g.x, my - g.y) < 22) {
                    gold += g.value;
                    g.life = 0;
                    return;
                }
            }

            // Click HUD destra
            if (mx >= GAME_W) { handleHUD(my); return; }

            // Click su griglia → piazza torre
            int col  = (int)(mx / CELL_W);
            int lane = (int)(my / CELL_H);
            if (col < 1 || col >= COLS || lane < 0 || lane >= LANES) return; // col 0 = castello
            if (grid[col][lane] != null) return;
            int cost = (selectedTower == Warrior.ARCHER) ? 50 : 70;
            if (gold < cost) return;
            gold -= cost;
            grid[col][lane] = new Warrior(col, lane, selectedTower);
        });

        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ESCAPE) {
                if (state == GameState.PLAYING) state = GameState.PAUSED;
                else if (state == GameState.PAUSED) state = GameState.PLAYING;
            }
            if (e.getCode() == KeyCode.ENTER) {
                if (state == GameState.MAIN_MENU || state == GameState.GAME_OVER || state == GameState.VICTORY)
                    startGame();
            }
            if (e.getCode() == KeyCode.DIGIT1) selectedTower = Warrior.ARCHER;
            if (e.getCode() == KeyCode.DIGIT2) selectedTower = Warrior.MELEE;
        });
    }

    private void handleHUD(double my) {
        if (my >= 200 && my < 270) selectedTower = Warrior.ARCHER;
        if (my >= 280 && my < 350) selectedTower = Warrior.MELEE;
    }

    private boolean inRect(double mx, double my, double x, double y, double w, double h) {
        return mx>=x && mx<=x+w && my>=y && my<=y+h;
    }

    // INIT PARTITA
    private void startGame() {
        for (int c=0;c<COLS;c++) for (int l=0;l<LANES;l++) grid[c][l] = null;
        enemies.clear(); bullets.clear(); coins.clear(); pawns.clear(); explosions.clear();
        gold = 150; castleHp = CASTLE_MAX;
        wave = 0; spawned = 0; spawnTimer = 0;
        prepareNextWave();
    }

    private void prepareNextWave() {
        if (wave >= TOTAL_WAVES) {
            state = GameState.VICTORY;
            return;
        }
        waveActive = false;
        pauseTimer = 3.0;
        state = GameState.WAVE_INTRO;
    }

    // UPDATE
 
    public void update(double delta) {
        if (state == GameState.PAUSED || state == GameState.MAIN_MENU || state == GameState.GAME_OVER || state == GameState.VICTORY)
            return;

        // Animazione globale ~8fps
        animTimer += delta;
        if (animTimer > 1.0/8.0) { animTimer=0; animFrame=(animFrame+1)%8; }

        if (state == GameState.WAVE_INTRO) {
            pauseTimer -= delta;
            if (pauseTimer <= 0) {
                wave++;
                waveActive = true;
                spawned = 0;
                spawnTimer = 0;
                state = GameState.PLAYING;
            }
            return;
        }

        updateWaves(delta);
        updateTowers(delta);
        updateEnemies(delta);
        updateBullets(delta);
        updateCoins(delta);
        updatePawns(delta);
        updateExplosions(delta);
        checkVictory();
    }

    private void updateWaves(double delta) {
        if (!waveActive) return;

        spawnTimer += delta;
        int total = wave * 3 + 2;
        double interval = Math.max(1.2, 3.5 - wave * 0.4);
        if (spawned < total && spawnTimer >= interval) {
            spawnTimer = 0;
            int lane = rng.nextInt(LANES);
            enemies.add(new Enemy(lane, GAME_W + 50, LANE_Y[lane]));
            spawned++;
        }

        boolean aliveEnemy = enemies.stream().anyMatch(enemy -> !enemy.dead);
        if (spawned >= total && !aliveEnemy && bullets.isEmpty()) {
            waveActive = false;
            prepareNextWave();
        }
    }

    private void updateTowers(double delta) {
        for (int c=0; c<COLS; c++) for (int l=0; l<LANES; l++) {
            Warrior t = grid[c][l];
            if (t == null) continue;

            // Animazione torre
            t.animTimer += delta;
            if (t.animTimer > 1.0/8.0) { t.animTimer = 0; t.animFrame = (t.animFrame + 1) % (t.shooting ? 8 : 6); }

            t.atkTimer += delta;
            double interval = (t.type == Warrior.ARCHER) ? 1.2 : 0.9;
            if (t.atkTimer < interval) continue;

            // Cerca nemico nella stessa corsia, più vicino alla torre
            Enemy target = null;
            double bestDist = Double.MAX_VALUE;
            for (Enemy e : enemies) {
                if (!e.dead && e.lane == l && e.x > t.cx) {
                    double d = e.x - t.cx;
                    if (d < bestDist) { bestDist = d; target = e; }
                }
            }
            if (target == null) { t.shooting = false; continue; }

            t.atkTimer = 0;
            t.shooting = true;

            if (t.type == Warrior.ARCHER) {
                Bullet b = new Bullet(t.cx + 20, t.cy, l);
                bullets.add(b);
            } else {
                if (bestDist < CELL_W * 1.5) {
                    target.hp -= 25;
                    explosions.add(new Explosion(target.x, target.y, assets.fxExplosion1 != null ? assets.fxExplosion1 : assets.fxExplosion2));
                    if (target.hp <= 0) killEnemy(target, true);
                }
            }
        }
    }

    private void updateEnemies(double delta) {
        for (Enemy e : enemies) {
            if (e.dead) { e.deadTimer += delta; continue; }

            // Animazione nemico
            e.animTimer += delta;
            int maxF = e.attacking ? 4 : 6;
            if (e.animTimer > 1.0/8.0) { e.animTimer = 0; e.animFrame = (e.animFrame + 1) % maxF; }

            if (e.attacking) {
                boolean blocked = false;
                for (int c=0; c<COLS; c++) {
                    Warrior t = grid[c][e.lane];
                    if (t != null && t.type == Warrior.MELEE && Math.abs(t.cx - e.x) < CELL_W) {
                        blocked = true;
                        e.atkTimer += delta;
                        if (e.atkTimer >= 1.0) {
                            e.atkTimer = 0;
                            t.hp -= 15;
                            if (t.hp <= 0) grid[c][e.lane] = null;
                        }
                        break;
                    }
                }
                if (!blocked) e.attacking = false;
                continue;
            }

            e.x -= e.speed * delta;

            for (int c=0; c<COLS; c++) {
                Warrior t = grid[c][e.lane];
                if (t != null && t.type == Warrior.MELEE && e.x < t.cx + CELL_W*0.6 && e.x > t.cx - CELL_W*0.6) {
                    e.attacking = true;
                    e.x = t.cx + CELL_W * 0.55;
                    break;
                }
            }

            if (e.x < CELL_W * 0.8) {
                castleHp--;
                killEnemy(e, false);
                if (castleHp <= 0) { castleHp = 0; state = GameState.GAME_OVER; }
            }
        }
        enemies.removeIf(e -> e.dead && e.deadTimer > 0.6);
    }

    private void updateBullets(double delta) {
        for (Bullet b : bullets) {
            b.x += b.speed * delta;
            for (Enemy e : enemies) {
                if (!e.dead && e.lane == b.targetLane && Math.abs(e.x - b.x) < 14 && Math.abs(e.y - b.y) < 30) {
                    e.hp -= b.damage;
                    explosions.add(new Explosion(e.x, e.y, assets.fxExplosion2 != null ? assets.fxExplosion2 : assets.fxExplosion1));
                    if (e.hp <= 0) killEnemy(e, true);
                    b.x = -999;
                    break;
                }
            }
        }
        bullets.removeIf(b -> b.x > GAME_W + 50 || b.x < 0);
    }

    private void updateCoins(double delta) {
        for (Gold g : coins) {
            g.y += g.vy * delta;
            g.vy += 60 * delta; 
            g.life -= delta;
        }
        coins.removeIf(g -> g.life <= 0);
    }

    private void updatePawns(double delta) {
        for (Pawn p : pawns) {
            p.timer += delta;
            if (p.timer >= 0.8) {
                gold += p.value;
                p.active = false;
                coins.removeIf(g -> Math.hypot(g.x - p.x, g.y - p.y) < 20);
            }
        }
        pawns.removeIf(p -> !p.active);
    }

    private void updateExplosions(double delta) {
        for (Explosion explosion : explosions) {
            explosion.life -= delta;
        }
        explosions.removeIf(explosion -> explosion.life <= 0);
    }

    private void killEnemy(Enemy e, boolean depositGold) {
        if (e.dead) return;
        e.dead = true;
        explosions.add(new Explosion(e.x, e.y, assets.fxExplosion1 != null ? assets.fxExplosion1 : assets.fxExplosion2));
        if (depositGold) {
            coins.add(new Gold(e.x, e.y - 20, e.reward));
            pawns.add(new Pawn(e.x, e.y - 20, e.reward));
        }
    }

    private void checkVictory() {
        if (wave >= TOTAL_WAVES && !waveActive && enemies.stream().noneMatch(enemy -> !enemy.dead) && bullets.isEmpty())
            state = GameState.VICTORY;
    }

    // RENDER
    public void render() {
        gc.setFill(Color.web("#1a1a2e"));
        gc.fillRect(0, 0, Main.WIDTH, Main.HEIGHT);

        switch (state) {
            case MAIN_MENU -> renderMenu();
            case PLAYING   -> renderGame();
            case WAVE_INTRO-> { renderGame(); renderOverlay("ONDATA " + (Math.min(wave + 1, TOTAL_WAVES)) + " IN ARRIVO", "Prepara le difese"); }
            case PAUSED    -> { renderGame(); renderOverlay("PAUSA", "ESC per continuare"); }
            case GAME_OVER -> renderEnd(false);
            case VICTORY   -> renderEnd(true);
        }
    }

    private void renderGame() {
        renderBackground();
        renderExplosions();
        renderTowers();
        renderEnemies();
        renderBullets();
        renderPawns();
        renderCoins();
        renderCastle();
        renderHoverCell();
        renderHUD();
        renderWaveInfo();
    }

    // Sfondo
    private void renderBackground() {
        for (int c = 0; c < COLS; c++) {
            for (int l = 0; l < LANES; l++) {
                double x = c * CELL_W, y = l * CELL_H;
                if (assets.imgGrass != null) {
                    gc.drawImage(assets.imgGrass, x, y, CELL_W, CELL_H);
                } else {
                    gc.setFill(l%2==0 ? Color.web("#4a7c3f") : Color.web("#3d6835"));
                    gc.fillRect(x, y, CELL_W, CELL_H);
                }
                
                if (l % 2 == 1) {
                    gc.setFill(Color.web("#00000018"));
                    gc.fillRect(x, y, CELL_W, CELL_H);
                }
            }
        }
        
        gc.setStroke(Color.web("#00000025"));
        gc.setLineWidth(1);
        for (int l=1;l<LANES;l++) gc.strokeLine(0, l*CELL_H, GAME_W, l*CELL_H);
        for (int c=1;c<COLS;c++) gc.strokeLine(c*CELL_W, 0, c*CELL_W, Main.HEIGHT);
    }

    // Castello
    private void renderCastle() {
        double cw = 160, ch = 130;
        double cx = CELL_W * 0.5 - cw/2;
        double cy = Main.HEIGHT/2.0 - ch/2;
        if (assets.imgCastle != null) {
            gc.drawImage(assets.imgCastle, cx, cy, cw, ch);
        } else {
            gc.setFill(Color.web("#7777cc")); gc.fillRect(cx, cy, cw, ch);
        }
        // Barra HP
        double bw = 140, bx = cx - 10, by = cy + ch + 4;
        double pct = (double)castleHp / CASTLE_MAX;
        gc.setFill(Color.web("#330000")); gc.fillRoundRect(bx, by, bw, 10, 4, 4);
        gc.setFill(pct>0.5?Color.web("#2ECC40"):pct>0.25?Color.web("#FFDC00"):Color.web("#FF4136"));
        gc.fillRoundRect(bx, by, bw*pct, 10, 4, 4);
        gc.setFill(Color.WHITE); gc.setFont(Font.font("Sans Serif", 9));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(castleHp+"/"+CASTLE_MAX, bx+bw/2, by+9);
    }

    // Torri
    private void renderTowers() {
        for (int c=0;c<COLS;c++) for (int l=0;l<LANES;l++) {
            Warrior t = grid[c][l];
            if (t == null) continue;

            // Edificio sfondo
            Image bldg = (t.type==Warrior.ARCHER) ? assets.imgArchery : assets.imgTower;
            if (bldg != null) {
                double bw = CELL_W * 0.85, bh = CELL_H * 0.95;
                gc.drawImage(bldg, t.cx-bw/2, t.cy-bh*0.6, bw, bh);
            }

            // Sprite unità animata
            Image sheet = t.type == Warrior.ARCHER
                ? (t.shooting ? assets.sheetArcherBlueShoot : assets.sheetArcherBlueIdle)
                : assets.sheetWarriorBlueIdle;
            int frames = t.type == Warrior.ARCHER ? (t.shooting ? 8 : 6) : 8;
            int frame  = t.animFrame % frames;
            drawSprite(sheet, frame, t.cx, t.cy + CELL_H*0.1, SPRITE, false);

            if (t.hp < t.maxHp) {
                drawHpBar(t.cx, t.cy - SPRITE/2 - 6, SPRITE * 0.8, (double)t.hp / t.maxHp, Color.web("#33FF33"));
            }
        }
    }

    // Nemici
    private void renderEnemies() {
        for (Enemy e : enemies) {
            double alpha = e.dead ? Math.max(0, 1.0 - e.deadTimer/0.5) : 1.0;
            gc.setGlobalAlpha(alpha);

            Image sheet = e.dead ? assets.sheetEnemyIdle
                        : e.attacking ? assets.sheetEnemyAttack
                        : assets.sheetEnemyRun;
            int frames  = e.dead ? 8 : e.attacking ? 4 : 6;
            int frame   = e.animFrame % frames;
            drawSprite(sheet, frame, e.x, e.y, SPRITE, true);

            gc.setGlobalAlpha(1.0);

            if (!e.dead) drawHpBar(e.x, e.y - SPRITE/2 - 6, SPRITE*0.8, (double)e.hp/e.maxHp, Color.web("#FF3333"));
        }
    }

    // Proiettili
    private void renderBullets() {
        for (Bullet b : bullets) {
            if (assets.imgArrow != null) {
                gc.drawImage(assets.imgArrow, b.x-10, b.y-5, 20, 10);
            } else {
                gc.setFill(Color.YELLOW);
                gc.fillOval(b.x-5, b.y-3, 10, 6);
            }
        }
    }

    // Monete
    private void renderCoins() {
        for (Gold g : coins) {
            double alpha = Math.min(1.0, g.life / 1.0);
            gc.setGlobalAlpha(alpha);
            if (assets.imgGold != null) {
                gc.drawImage(assets.imgGold, g.x-14, g.y-14, 28, 28);
            } else {
                gc.setFill(Color.GOLD); gc.fillOval(g.x-10, g.y-10, 20, 20);
            }
            gc.setFill(Color.web("#FFD700")); gc.setFont(Font.font("Bold",10));
            gc.setTextAlign(TextAlignment.CENTER);
            gc.fillText("+"+g.value, g.x, g.y-17);
            gc.setGlobalAlpha(1.0);
        }
    }

    private void renderPawns() {
        for (Pawn p : pawns) {
            double alpha = Math.min(1.0, p.timer / 0.8);
            double size = 24;
            if (assets.pawnIdleGold != null) {
                gc.setGlobalAlpha(0.7 + 0.3 * alpha);
                gc.drawImage(assets.pawnIdleGold, p.x - size/2, p.y - size/2, size, size);
                gc.setGlobalAlpha(1.0);
            } else {
                gc.setGlobalAlpha(0.7 + 0.3 * alpha);
                gc.setFill(Color.web("#66ccff"));
                gc.fillOval(p.x - 10, p.y - 10, 20, 20);
                gc.setFill(Color.WHITE);
                gc.setFont(Font.font(10));
                gc.setTextAlign(TextAlignment.CENTER);
                gc.fillText("♟", p.x, p.y + 4);
                gc.setGlobalAlpha(1.0);
            }
        }
    }

    private void renderExplosions() {
        for (Explosion explosion : explosions) {
            double alpha = Math.max(0, Math.min(1, explosion.life / 0.45));
            double size = 40 + 30 * (1 - alpha);
            if (explosion.image != null) {
                gc.setGlobalAlpha(alpha);
                gc.drawImage(explosion.image, explosion.x - size/2, explosion.y - size/2, size, size);
                gc.setGlobalAlpha(1.0);
            } else {
                gc.setFill(Color.rgb(255, 170, 51, alpha));
                gc.fillOval(explosion.x - size/2, explosion.y - size/2, size, size);
            }
        }
    }

    // Hover cella
    private void renderHoverCell() {
        if (mouseX >= GAME_W || mouseX < CELL_W) return;
        int hc = (int)(mouseX/CELL_W), hl = (int)(mouseY/CELL_H);
        if (hc<1||hc>=COLS||hl<0||hl>=LANES) return;
        boolean occ = grid[hc][hl] != null;
        int cost = selectedTower==Warrior.ARCHER ? 50 : 70;
        gc.setFill(occ ? Color.web("#FF000033") : gold>=cost ? Color.web("#FFFFFF33") : Color.web("#FF880033"));
        gc.fillRect(hc*CELL_W, hl*CELL_H, CELL_W, CELL_H);
    }

    // Info ondata in alto al centro
    private void renderWaveInfo() {
        String msg = waveActive
            ? "⚔  Ondata " + wave + " / " + TOTAL_WAVES + "  — nemici: " + enemies.stream().filter(e->!e.dead).count()
            : (wave < TOTAL_WAVES ? "Prossima ondata tra " + (int)Math.ceil(pauseTimer) + "s" : "");
        if (msg.isEmpty()) return;
        gc.setFill(Color.web("#00000099")); gc.fillRoundRect(250, 8, 460, 32, 8, 8);
        gc.setFill(Color.web("#FFD700")); gc.setFont(Font.font("Serif", 15));
        gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(msg, 480, 30);
    }

    // HUD destra
    private void renderHUD() {
        double hx = GAME_W + 14;
        gc.setFill(Color.web("#0e0e22")); gc.fillRect(GAME_W, 0, Main.WIDTH-GAME_W, Main.HEIGHT);
        gc.setStroke(Color.web("#C9A84C55")); gc.setLineWidth(1);
        gc.strokeLine(GAME_W, 0, GAME_W, Main.HEIGHT);

        gc.setTextAlign(TextAlignment.LEFT);
        gc.setFill(Color.web("#C9A84C")); gc.setFont(Font.font("Serif", 20));
        gc.fillText("⚔  IMPERIA", hx, 40);
        hline(48);

        gc.setFill(Color.web("#FFD700")); gc.setFont(Font.font("Serif", 15));
        gc.fillText("🪙 Oro:  " + gold, hx, 75);
        gc.setFill(Color.web("#aaaacc"));
        gc.fillText("Ondata: " + Math.min(wave,TOTAL_WAVES) + " / " + TOTAL_WAVES, hx, 95);
        hline(115);

        gc.setFill(Color.web("#C9A84C")); gc.setFont(Font.font("Serif", 13));
        gc.fillText("DIFENSORI  [1]  [2]", hx, 140);

        // Bottone Arciere
        boolean s0 = selectedTower == Warrior.ARCHER;
        gc.setFill(s0 ? Color.web("#C9A84C22") : Color.web("#ffffff0a"));
        gc.fillRoundRect(hx-4, 150, 290, 80, 6, 6);
        if (s0) { gc.setStroke(Color.web("#C9A84C77")); gc.strokeRoundRect(hx-4, 150, 290, 80, 6, 6); }
        drawSprite(assets.sheetArcherBlueIdle, animFrame%6, hx+34, 192, 60, false);
        gc.setFill(s0?Color.web("#FFD700"):Color.web("#dddddd")); gc.setFont(Font.font("Serif",13));
        gc.fillText("1 · Arciere", hx+70, 175);
        gc.setFill(gold>=50?Color.web("#C9A84C"):Color.web("#FF6666")); gc.setFont(Font.font(11));
        gc.fillText("Costo: 50🪙  HP: 80", hx+70, 195);
        gc.setFill(Color.web("#888888")); gc.setFont(Font.font(10));
        gc.fillText("Spara frecce a distanza", hx+70, 213);

        // Bottone Guerriero
        boolean s1 = selectedTower == Warrior.MELEE;
        gc.setFill(s1 ? Color.web("#C9A84C22") : Color.web("#ffffff0a"));
        gc.fillRoundRect(hx-4, 240, 290, 80, 6, 6);
        if (s1) { gc.setStroke(Color.web("#C9A84C77")); gc.strokeRoundRect(hx-4, 240, 290, 80, 6, 6); }
        drawSprite(assets.sheetWarriorBlueIdle, animFrame%8, hx+34, 282, 60, false);
        gc.setFill(s1?Color.web("#FFD700"):Color.web("#dddddd")); gc.setFont(Font.font("Serif",13));
        gc.fillText("2 · Guerriero", hx+70, 265);
        gc.setFill(gold>=70?Color.web("#C9A84C"):Color.web("#FF6666")); gc.setFont(Font.font(11));
        gc.fillText("Costo: 70🪙  HP: 160", hx+70, 285);
        gc.setFill(Color.web("#888888")); gc.setFont(Font.font(10));
        gc.fillText("Blocca i nemici c/c", hx+70, 303);

        hline(335);
        gc.setFill(Color.web("#666688")); gc.setFont(Font.font(11));
        gc.fillText("Click cella → piazza", hx, 355);
        gc.fillText("Click moneta → raccogli", hx, 371);
        gc.fillText("ESC = pausa", hx, 387);

        // Barra castello
        hline(410);
        gc.setFill(Color.web("#C9A84C")); gc.setFont(Font.font("Serif",13));
        gc.fillText("🏰 Castello", hx, 430);
        double bw = Main.WIDTH-GAME_W-28, pct = (double)castleHp/CASTLE_MAX;
        gc.setFill(Color.web("#330000")); gc.fillRoundRect(hx, 437, bw, 14, 4, 4);
        gc.setFill(pct>0.5?Color.web("#2ECC40"):pct>0.25?Color.web("#FFDC00"):Color.web("#FF4136"));
        gc.fillRoundRect(hx, 437, bw*pct, 14, 4, 4);
        gc.setFill(Color.WHITE); gc.setFont(Font.font(10)); gc.setTextAlign(TextAlignment.CENTER);
        gc.fillText(castleHp+"/"+CASTLE_MAX, GAME_W+(Main.WIDTH-GAME_W)/2.0, 449);
    }

    private void hline(double y) {
        gc.setStroke(Color.web("#C9A84C33")); gc.setLineWidth(1);
        gc.strokeLine(GAME_W+6, y, Main.WIDTH-6, y);
    }

    // Menu
    private void renderMenu() {
        gc.setFill(Color.web("#06111e")); gc.fillRect(0, 0, Main.WIDTH, Main.HEIGHT);
        gc.setFill(Color.web("#10234a")); gc.fillRoundRect(140, 80, Main.WIDTH-280, Main.HEIGHT-160, 40, 40);
        gc.setFill(Color.web("#ffffff11")); gc.fillOval(220, 50, 320, 320);
        gc.setFill(Color.web("#ffffff08")); gc.fillOval(760, 160, 420, 420);

        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#F8E2A0")); gc.setFont(Font.font("Serif",72));
        gc.fillText("IMPERIA", Main.WIDTH/2.0, 190);
        gc.setFill(Color.web("#ffffffaa")); gc.setFont(Font.font("Serif",24));
        gc.fillText("Tower Defense epico", Main.WIDTH/2.0, 238);

        gc.setFill(Color.web("#dcdcff")); gc.setFont(Font.font("Sans Serif",14));
        gc.fillText("Costruisci il tuo esercito, raccogli oro e difendi il castello.", Main.WIDTH/2.0, 282);
        gc.fillText("Sopravvivi a " + TOTAL_WAVES + " ondate e mostra chi comanda.", Main.WIDTH/2.0, 306);

        gc.setFill(Color.web("#ffffff22")); gc.fillRoundRect(260, 330, Main.WIDTH-520, 220, 24, 24);
        gc.setStroke(Color.web("#ffffff33")); gc.setLineWidth(1.5);
        gc.strokeRoundRect(260, 330, Main.WIDTH-520, 220, 24, 24);

        boolean hover = inRect(mouseX, mouseY, 470, 380, 340, 64);
        gc.setFill(hover ? Color.web("#C9A84C") : Color.web("#7f6d2c"));
        gc.fillRoundRect(470, 380, 340, 64, 18, 18);
        gc.setStroke(Color.web("#ffffff33")); gc.setLineWidth(2);
        gc.strokeRoundRect(470, 380, 340, 64, 18, 18);
        gc.setFill(Color.web("#0f1728")); gc.setFont(Font.font("Serif",24));
        gc.fillText("INIZIA PARTITA", Main.WIDTH/2.0, 426);
        gc.setFill(Color.web("#dde0f3")); gc.setFont(Font.font(13));
        gc.fillText("Premi INVIO o clicca per partire", Main.WIDTH/2.0, 454);

        if (assets.sheetArcherBlueIdle != null) drawSprite(assets.sheetArcherBlueIdle, animFrame%6, Main.WIDTH/2.0-140, 560, 80, false);
        if (assets.sheetWarriorBlueIdle != null) drawSprite(assets.sheetWarriorBlueIdle, animFrame%8, Main.WIDTH/2.0, 560, 80, false);
        if (assets.sheetEnemyRun != null) drawSprite(assets.sheetEnemyRun, animFrame%6, Main.WIDTH/2.0+140, 560, 80, true);

        gc.setFill(Color.web("#ffffff66")); gc.setFont(Font.font(11)); gc.setTextAlign(TextAlignment.RIGHT);
        gc.fillText("Premi 1/2 per selezionare il difensore.", Main.WIDTH - 18, Main.HEIGHT - 26);
    }

    private void renderOverlay(String title, String sub) {
        gc.setFill(Color.web("#00000099")); gc.fillRect(0,0,Main.WIDTH,Main.HEIGHT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web("#C9A84C")); gc.setFont(Font.font("Serif",52));
        gc.fillText(title, Main.WIDTH/2.0, Main.HEIGHT/2.0-20);
        gc.setFill(Color.web("#ffffff88")); gc.setFont(Font.font(16));
        gc.fillText(sub, Main.WIDTH/2.0, Main.HEIGHT/2.0+30);
    }

    private void renderEnd(boolean win) {
        gc.setFill(Color.web(win?"#071a07":"#1a0707")); gc.fillRect(0,0,Main.WIDTH,Main.HEIGHT);
        gc.setTextAlign(TextAlignment.CENTER);
        gc.setFill(Color.web(win?"#C9A84C":"#FF4136")); gc.setFont(Font.font("Serif",58));
        gc.fillText(win?"IMPERIA È SALVA!":"IL CASTELLO È CADUTO", Main.WIDTH/2.0, Main.HEIGHT/2.0-30);
        gc.setFill(Color.web("#dddddd")); gc.setFont(Font.font(18));
        gc.fillText(win?"Hai respinto tutte le ondate!":"I nemici hanno vinto.", Main.WIDTH/2.0, Main.HEIGHT/2.0+20);
        gc.setFill(Color.web("#ffffff55")); gc.setFont(Font.font(14));
        gc.fillText("Clicca o INVIO per ricominciare", Main.WIDTH/2.0, Main.HEIGHT/2.0+65);
    }

  

    /**
     * Disegna un singolo frame da uno spritesheet 192x192.
     * Se lo sheet è null disegna un rettangolo colorato come fallback.
     */
    private void drawSprite(Image sheet, int frame, double cx, double cy, double size, boolean flipH) {
        if (sheet == null) {
            gc.setFill(flipH ? Color.RED : Color.CORNFLOWERBLUE);
            gc.fillOval(cx-size/2, cy-size/2, size, size);
            return;
        }
        double sx = frame * 192.0;
        double dx = cx - size/2, dy = cy - size/2;
        if (flipH) {
            gc.save();
            gc.translate(cx, cy);
            gc.scale(-1, 1);
            gc.drawImage(sheet, sx, 0, 192, 192, -size/2, -size/2, size, size);
            gc.restore();
        } else {
            gc.drawImage(sheet, sx, 0, 192, 192, dx, dy, size, size);
        }
    }

    private void drawHpBar(double cx, double y, double w, double pct, Color fill) {
        gc.setFill(Color.web("#00000066")); gc.fillRect(cx-w/2, y, w, 5);
        gc.setFill(fill); gc.fillRect(cx-w/2, y, w*Math.max(0,pct), 5);
    }
}
