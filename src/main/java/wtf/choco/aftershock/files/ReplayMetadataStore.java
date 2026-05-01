package wtf.choco.aftershock.files;

import wtf.choco.aftershock.replay.IReplay;
import wtf.choco.aftershock.replay.ReplayMetadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ReplayMetadataStore implements ReplayMetadataAccessor {

    private final Map<String, ReplayMetadata> replayMetadata = new HashMap<>();

    public void setReplayMetadata(String replayId, ReplayMetadata metadata) {
        this.replayMetadata.put(replayId, metadata);
    }

    @Override
    public ReplayMetadata getReplayMetadata(IReplay replay) {
        return replayMetadata.computeIfAbsent(replay.id(), _ -> new ReplayMetadata());
    }

    @Override
    public Collection<ReplayMetadataEntry> getAllReplayMetadata() {
        return replayMetadata.entrySet().stream()
                .map(entry -> new ReplayMetadataEntry(entry.getKey(), entry.getValue()))
                .toList();
    }

}
