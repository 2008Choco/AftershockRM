package wtf.choco.aftershock.files;

public interface ReplayFileWatcherListener {

    public void stop();

    public void pushIgnoreIncomingEventsTicket();

    public void popIgnoreIncomingEventsTicket();

}
