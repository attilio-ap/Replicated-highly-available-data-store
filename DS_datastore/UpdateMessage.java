package DS_datastore;

import java.io.Serializable;

public class UpdateMessage implements ReplicableMessage {
    private String key;
    private String value;
    private String originServerId;
    private VectorClock vectorClock;

    public UpdateMessage(String key, String value, String originServerId, VectorClock vectorClock) {
        this.key = key;
        this.value = value;
        this.originServerId = originServerId;
        this.vectorClock = new VectorClock(vectorClock); // copy the VC
    }

    public String getKey() { return key; }
    public String getValue() { return value; }
    public String getOriginServerId() { return originServerId; }
    public VectorClock getVectorClock() { return vectorClock; }
}
