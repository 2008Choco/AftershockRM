package wtf.choco.aftershock.files;

import wtf.choco.aftershock.replay.IReplay;
import wtf.choco.aftershock.replay.ReplayMetadata;

import java.util.Collection;

public interface ReplayMetadataAccessor {

    public ReplayMetadata getReplayMetadata(IReplay replay);

    public Collection<ReplayMetadataEntry> getAllReplayMetadata();

    public record ReplayMetadataEntry(String replayId, ReplayMetadata metadata) { }

}
