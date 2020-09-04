package me.thesourcecode.sourcebot.api.utility;

import net.dv8tion.jda.api.events.GenericEvent;

import java.util.function.Consumer;

public abstract class AbstractListener<T extends GenericEvent> implements Consumer<T> {
    private final Class<T> clazz;

    public AbstractListener(Class<T> clazz) {
        this.clazz = clazz;
    }

    public void listen(Listener listener) {
        listener.handle(clazz, this);
    }
}
