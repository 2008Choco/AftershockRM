package wtf.choco.aftershock.util;

import javafx.beans.property.ObjectPropertyBase;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.event.EventType;

import java.util.function.BiConsumer;

public final class SimpleEventProperty<E extends Event> extends ObjectPropertyBase<EventHandler<E>> {

    private final BiConsumer<EventType<E>, EventHandler<? super E>> eventHandlerSetter;
    private final EventType<E> eventType;
    private final Object bean;
    private final String name;

    public SimpleEventProperty(BiConsumer<EventType<E>, EventHandler<? super E>> eventHandlerSetter, EventType<E> eventType, Object bean, String name) {
        this.eventHandlerSetter = eventHandlerSetter;
        this.eventType = eventType;
        this.bean = bean;
        this.name = name;
    }

    @Override
    protected void invalidated() {
        this.eventHandlerSetter.accept(eventType, get());
    }

    @Override
    public Object getBean() {
        return bean;
    }

    @Override
    public String getName() {
        return name;
    }

}
