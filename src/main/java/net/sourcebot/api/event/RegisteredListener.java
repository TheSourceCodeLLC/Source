package net.sourcebot.api.event;

import net.dv8tion.jda.api.events.GenericEvent;
import net.sourcebot.api.module.SourceModule;

import java.util.function.Consumer;

class RegisteredListener<T extends GenericEvent> {
    final SourceModule owner;
    final Consumer<T> consumer;

    RegisteredListener(SourceModule owner, Consumer<T> consumer) {
        this.owner = owner;
        this.consumer = consumer;
    }
}
