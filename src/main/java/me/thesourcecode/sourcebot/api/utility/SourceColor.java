package me.thesourcecode.sourcebot.api.utility;

import java.awt.*;

public enum SourceColor {
    BLUE(rgb(52, 152, 219)),
    GREEN(rgb(46, 204, 113)),
    RED(rgb(231, 76, 60)),
    PURPLE(rgb(155, 89, 182)),
    ORANGE(rgb(230, 126, 34));

    private final Color color;

    SourceColor(Color color) {
        this.color = color;
    }

    private static Color rgb(int r, int g, int b) {
        return new Color(r, g, b);
    }

    public Color asColor() {
        return color;
    }
}
