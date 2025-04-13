package DS_datastore;

import java.io.Serializable;

public class ValueEntry implements Serializable {
    private String value;
    private VectorClock vClock;

    public ValueEntry(String value, VectorClock vClock) {
        this.value = value;
        this.vClock = vClock; // make a copy
    }

    public String getValue() {
        return value;
    }

    public VectorClock getVectorClock() {
        return vClock;
    }

    @Override
    public String toString() {
        return "Value: " + value + ", VC: " + vClock.toString();
    }
}
