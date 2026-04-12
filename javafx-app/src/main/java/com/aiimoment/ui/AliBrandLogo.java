package com.aiimoment.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;

/**
 * 矢量品牌标：取意 “ali”，青绿—蓝渐变带状造型。
 * 若有 {@code /images/ali-logo.png}，可在 {@link com.aiimoment.controller.MainController} 中优先加载位图。
 */
public final class AliBrandLogo {

    private AliBrandLogo() {
    }

    public static Node build(double size) {
        Group g = new Group();

        Polygon ribbon = new Polygon(
                18, 4,
                28, 6,
                26, 12,
                20, 10,
                22, 26,
                16, 28,
                14, 14,
                18, 12
        );
        ribbon.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#00e5ff")),
                new Stop(0.55, Color.web("#00acc1")),
                new Stop(1, Color.web("#1565c0"))));

        Polygon leg = new Polygon(
                4, 26,
                9, 8,
                14, 8,
                10, 26
        );
        leg.setFill(new LinearGradient(0, 1, 0.4, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#00838f")),
                new Stop(1, Color.web("#4dd0e1"))));

        Polygon bar = new Polygon(
                7, 16,
                17, 16,
                16.5, 19,
                7.5, 19
        );
        bar.setFill(new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE,
                new Stop(0, Color.web("#26c6da")),
                new Stop(1, Color.web("#0277bd"))));

        Polygon dot = new Polygon(
                23, 6.5, 25, 6.5, 25, 8.5, 23, 8.5
        );
        dot.setFill(Color.web("#80deea"));

        Rectangle clip = new Rectangle(32, 32);
        clip.setArcWidth(8);
        clip.setArcHeight(8);
        g.setClip(clip);

        g.getChildren().addAll(ribbon, leg, bar, dot);

        double s = size / 32.0;
        g.setScaleX(s);
        g.setScaleY(s);
        return g;
    }
}
