package me.thesourcecode.sourcebot.api.message.alerts;

import me.thesourcecode.sourcebot.api.utility.SourceColor;

public class CriticalAlert extends ColoredAlert {

    public CriticalAlert() {
        super(SourceColor.RED);
    }
}
