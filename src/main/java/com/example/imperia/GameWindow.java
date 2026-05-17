package com.example.imperia;

import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class GameWindow {

    private final Stage          stage;
    private final Canvas         canvas;
    private final GraphicsContext gc;
    private final GameLoop       gameLoop;
    private final GameManager    gameManager;

    public GameWindow(Stage stage) {
        this.stage = stage;

        canvas = new Canvas(Main.WIDTH, Main.HEIGHT);
        gc     = canvas.getGraphicsContext2D();

        StackPane root = new StackPane(canvas);
        root.setStyle("-fx-background-color: #1a1a2e;");

        Scene scene = new Scene(root, Main.WIDTH, Main.HEIGHT);

        stage.setTitle(Main.TITLE);
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMaximized(true);

        gameManager = new GameManager(gc, scene, canvas);
        gameLoop    = new GameLoop(gameManager);
    }

    public void show() {
        stage.show();
        gameLoop.start();
    }
}
