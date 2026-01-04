package wtf.choco.aftershock.event;

import javafx.beans.NamedArg;
import javafx.event.Event;
import javafx.event.EventTarget;
import javafx.event.EventType;

public final class ReplayBinDisplayEvent extends Event {

    public static final EventType<ReplayBinDisplayEvent> ANY = new EventType<>(Event.ANY, "ANY");

    public static final EventType<ReplayBinDisplayEvent> CLONE = new EventType<>(ANY, "CLONE");
    public static final EventType<ReplayBinDisplayEvent> DELETE = new EventType<>(ANY, "DELETE");
    public static final EventType<ReplayBinDisplayEvent> HIDE = new EventType<>(ANY, "HIDE");

    private final boolean alert;

    public ReplayBinDisplayEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target, @NamedArg("eventType") EventType<? extends Event> eventType, @NamedArg("alert") boolean alert) {
        super(source, target, eventType);
        this.alert = alert;
    }

    public ReplayBinDisplayEvent(@NamedArg("source") Object source, @NamedArg("target") EventTarget target, @NamedArg("eventType") EventType<? extends Event> eventType) {
        this(source, target, eventType, false);
    }

    public ReplayBinDisplayEvent(@NamedArg("eventType") EventType<? extends Event> eventType, @NamedArg("alert") boolean alert) {
        super(eventType);
        this.alert = alert;
    }

    public ReplayBinDisplayEvent(@NamedArg("eventType") EventType<? extends Event> eventType) {
        this(eventType, false);
    }

    // Only used for DELETE event
    public boolean shouldAlert() {
        return alert;
    }

    @Override
    public String toString() {
        return "ReplayBinDisplayEvent [source = %s, target = %s, eventType = %s, alert = %s]".formatted(getSource(), getTarget(), getEventType(), alert);
    }

    @Override
    public ReplayBinDisplayEvent copyFor(Object newSource, EventTarget newTarget) {
        return (ReplayBinDisplayEvent) super.copyFor(newSource, newTarget);
    }

}
