package me.thesourcecode.sourcebot.api.utility;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;

import java.util.Collection;
import java.util.function.Consumer;

public class Listener implements EventListener {
    private final Multimap<Class<? extends GenericEvent>, Consumer<GenericEvent>> eventMap = ArrayListMultimap.create();

    public Listener(JDA jda) {
        jda.addEventListener(this);
    }

    /***
     *
     * @param event The event to manually fire
     */
    public static void callEvent(GenericEvent event) {
        event.getJDA().getEventManager().handle(event);
    }

    /***
     *
     * @param type The JDA Event class to listen for
     * @param consumer The consumer responsible for executing the event
     * @param <E> Generic bound for JDA Events
     * @return This Listener, for chaining
     */
    public <E extends GenericEvent> Listener handle(Class<E> type, Consumer<E> consumer) {
        eventMap.put(type, (Consumer<GenericEvent>) consumer);
        return this;
    }

    @Override
    public void onEvent(GenericEvent event) {
        Collection<Consumer<GenericEvent>> consumers = eventMap.get(event.getClass());
        if (consumers != null) {
            consumers.forEach(consumer -> consumer.accept(event));
        }
    }

}
