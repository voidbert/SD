import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MultiPutRequestMessage extends Message {
    private int                 id;
    private Map<String, byte[]> map;

    public MultiPutRequestMessage(int id, Map<String, byte[]> map) {
        this.id  = id;
        this.map = map;
    }

    public static MultiPutRequestMessage messageDeserialize(DataInputStream in) {
        int                 id      = in.readInt();
        int                 mapSize = in.readInt();
        Map<String, byte[]> map     = new HashMap<>();
        for (int i = 0; i < mapSize; i++) {
            String key         = in.readUTF();
            int    valueLength = in.readInt();
            byte[] value       = new byte[valueLength];
            in.readFully(value);
            map.put(key, value);
        }
        return new MultiPutRequestMessage(id, map);
    }

    protected void messageSerialize(DataOutputStream out) {
        out.writeInt(id);
        out.writeInt(map.size());
        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            out.writeUTF(entry.getKey());
            out.writeInt(entry.getValue().length);
            out.write(entry.getValue());
        }
    }

    public int getId() {
        return this.id;
    }

    public Map<String, byte[]> getMap() {
        return this.map;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if (other == null || getClass() != other.getClass())
            return false;
        MultiPutRequestMessage that = (MultiPutRequestMessage) other;
        return id == that.id && map.equals(that.map);
    }

    @Override
    public Object clone() {
        try {
            MultiPutRequestMessage cloned = (MultiPutRequestMessage) super.clone();
            cloned.map                    = new HashMap<>(this.map.size());
            for (Map.Entry<String, byte[]> entry : this.map.entrySet()) {
                cloned.map.put(entry.getKey(), entry.getValue().clone());
            }
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        StringBuilder mapString = new StringBuilder();
        mapString.append("{");
        for (Map.Entry<String, byte[]> entry : map.entrySet()) {
            mapString.append(entry.getKey())
                .append("=")
                .append(Arrays.toString(entry.getValue()))
                .append(", ");
        }
        if (mapString.length() > 1) {
            mapString.setLength(mapString.length() - 2);
        }
        mapString.append("}");

        String str = "MultiPutRequestMessage(Id= %d, Map= %s)";
        String res = String.format(str, this.id, mapString.toString());
        return res;
    }
}
