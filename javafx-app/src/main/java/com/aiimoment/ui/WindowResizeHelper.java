package com.aiimoment.ui;

import com.aiimoment.Main;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

/**
 * 为 {@link javafx.stage.StageStyle#UNDECORATED} 窗口提供四边与四角拖拽缩放（系统原生边框不可用时的常见做法）。
 */
public final class WindowResizeHelper {

    private static final double MIN_STAGE_W = 640;
    private static final double MIN_STAGE_H = 400;

    private enum Edge {
        NONE,
        N, NE, E, SE, S, SW, W, NW
    }

    private WindowResizeHelper() {
    }

    public static void install(Stage stage, Scene scene, int marginPx) {
        final Edge[] activeEdge = { Edge.NONE };
        final double[] pressScreenX = { 0 };
        final double[] pressScreenY = { 0 };
        final double[] startW = { 0 };
        final double[] startH = { 0 };
        final double[] startX = { 0 };
        final double[] startY = { 0 };

        scene.addEventFilter(MouseEvent.MOUSE_MOVED, e -> {
            if (stage.isMaximized()) {
                scene.setCursor(Cursor.DEFAULT);
                return;
            }
            if (activeEdge[0] != Edge.NONE) {
                scene.setCursor(cursorFor(activeEdge[0]));
                return;
            }
            Edge edge = detectEdge(scene, e.getSceneX(), e.getSceneY(), marginPx);
            scene.setCursor(cursorFor(edge));
        });

        scene.addEventFilter(MouseEvent.MOUSE_EXITED, e -> {
            if (activeEdge[0] == Edge.NONE) {
                scene.setCursor(Cursor.DEFAULT);
            }
        });

        scene.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
            if (stage.isMaximized()) {
                return;
            }
            Edge edge = detectEdge(scene, e.getSceneX(), e.getSceneY(), marginPx);
            if (edge == Edge.NONE) {
                return;
            }
            activeEdge[0] = edge;
            pressScreenX[0] = e.getScreenX();
            pressScreenY[0] = e.getScreenY();
            startW[0] = stage.getWidth();
            startH[0] = stage.getHeight();
            startX[0] = stage.getX();
            startY[0] = stage.getY();
            e.consume();
        });

        scene.addEventFilter(MouseEvent.MOUSE_DRAGGED, e -> {
            if (activeEdge[0] == Edge.NONE || stage.isMaximized()) {
                return;
            }
            double dx = e.getScreenX() - pressScreenX[0];
            double dy = e.getScreenY() - pressScreenY[0];
            double x = startX[0];
            double y = startY[0];
            double w = startW[0];
            double h = startH[0];

            switch (activeEdge[0]) {
                case E:
                    w = clampMin(startW[0] + dx, MIN_STAGE_W);
                    break;
                case S:
                    h = clampMin(startH[0] + dy, MIN_STAGE_H);
                    break;
                case SE:
                    w = clampMin(startW[0] + dx, MIN_STAGE_W);
                    h = clampMin(startH[0] + dy, MIN_STAGE_H);
                    break;
                case W:
                    w = clampMin(startW[0] - dx, MIN_STAGE_W);
                    x = startX[0] + startW[0] - w;
                    break;
                case N:
                    h = clampMin(startH[0] - dy, MIN_STAGE_H);
                    y = startY[0] + startH[0] - h;
                    break;
                case NW:
                    w = clampMin(startW[0] - dx, MIN_STAGE_W);
                    h = clampMin(startH[0] - dy, MIN_STAGE_H);
                    x = startX[0] + startW[0] - w;
                    y = startY[0] + startH[0] - h;
                    break;
                case NE:
                    w = clampMin(startW[0] + dx, MIN_STAGE_W);
                    h = clampMin(startH[0] - dy, MIN_STAGE_H);
                    y = startY[0] + startH[0] - h;
                    break;
                case SW:
                    w = clampMin(startW[0] - dx, MIN_STAGE_W);
                    h = clampMin(startH[0] + dy, MIN_STAGE_H);
                    x = startX[0] + startW[0] - w;
                    break;
                default:
                    break;
            }

            stage.setX(x);
            stage.setY(y);
            stage.setWidth(w);
            stage.setHeight(h);
            e.consume();
        });

        scene.addEventFilter(MouseEvent.MOUSE_RELEASED, e -> {
            if (activeEdge[0] != Edge.NONE) {
                activeEdge[0] = Edge.NONE;
                Main.syncSceneToStage(stage);
            }
        });
    }

    private static double clampMin(double v, double min) {
        return Math.max(min, v);
    }

    private static Edge detectEdge(Scene scene, double x, double y, int m) {
        double w = scene.getWidth();
        double h = scene.getHeight();
        if (w <= m * 2 || h <= m * 2) {
            return Edge.NONE;
        }
        boolean left = x < m;
        boolean right = x > w - m;
        boolean top = y < m;
        boolean bottom = y > h - m;

        if (top && left) {
            return Edge.NW;
        }
        if (top && right) {
            return Edge.NE;
        }
        if (bottom && left) {
            return Edge.SW;
        }
        if (bottom && right) {
            return Edge.SE;
        }
        if (top) {
            return Edge.N;
        }
        if (bottom) {
            return Edge.S;
        }
        if (left) {
            return Edge.W;
        }
        if (right) {
            return Edge.E;
        }
        return Edge.NONE;
    }

    private static Cursor cursorFor(Edge edge) {
        switch (edge) {
            case N:
            case S:
                return Cursor.N_RESIZE;
            case E:
            case W:
                return Cursor.E_RESIZE;
            case NE:
                return Cursor.NE_RESIZE;
            case NW:
                return Cursor.NW_RESIZE;
            case SE:
                return Cursor.SE_RESIZE;
            case SW:
                return Cursor.SW_RESIZE;
            default:
                return Cursor.DEFAULT;
        }
    }
}
