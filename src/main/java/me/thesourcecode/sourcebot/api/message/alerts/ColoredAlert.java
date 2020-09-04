package me.thesourcecode.sourcebot.api.message.alerts;

import me.thesourcecode.sourcebot.api.utility.SourceColor;

import java.awt.*;

public class ColoredAlert extends Alert {

    /***
     *
     * @param color The color of this alert
     */
    public ColoredAlert(Color color) {
        setColor(color);
    }

    /***
     *
     * @param color The SourceColor of this alert
     */
    public ColoredAlert(SourceColor color) {
        this(color.asColor());
    }
}
