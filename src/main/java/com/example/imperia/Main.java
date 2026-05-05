package com.example.imperia;

import javafx.application.Application;
import javafx.stage.Stage;

public class Main extends Application {

    public static final String TITLE   = "Tower Defense: Imperia";
    public static final int    WIDTH   = 1280;
    public static final int    HEIGHT  = 720;

    @Override
    public void start(Stage stage) {
        GameWindow window = new GameWindow(stage);
        window.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
