import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.HashSet;
import java.util.Set;

public class MultiGetRequestMessage extends Message {
    private int         id;
    private Set<String> keys;

    public MultiGetRequestMessage(int id, Set<String> keys) {
        this.id   = id;
        this.keys = keys;
    }

    public static MultiGetRequestMessage messageDeserialize(DataInputStream in) {
        int         id       = in.readInt();
        int         keysSize = in.readInt();
        Set<String> keys     = new HashSet<>();
        for (int i = 0; i < keysSize; i++) {
            keys.add(in.readUTF());
        }
        return new MultiGetRequestMessage(id, keys);
    }

    protected void messageSerialize(DataOutputStream out) {
        out.writeInt(id);
        out.writeInt(keys.size());
        for (String key : keys) {
            out.writeUTF(key);
        }
    }

    public int getId() {
        return this.id;
    }

    public Set<String> getKeys() {
        return this.keys;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if ((other == null) || (this.getClass() != other.getClass()))
            return false;
        MultiGetRequestMessage that = (MultiGetRequestMessage) other;
        return this.id == that.id && this.keys.equals(that.keys);
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    @Override
    public String toString() {
        String str = "MultiGetRequestMessage(ID= %d, Keys= %s)";
        String res = String.format(str, this.id, this.keys);
        return res;
    }
}
