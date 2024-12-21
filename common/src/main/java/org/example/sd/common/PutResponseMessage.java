import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Arrays;

public class PutResponseMessage extends Message {
    private int    requestId;
    private byte[] value;

    public PutResponseMessage(int requestId, byte[] value) {
        this.requestId = requestId;
        this.value     = value;
    }

    public static PutResponseMessage messageDeserialize(DataInputStream in) {
        int    requestId = in.readInt();
        byte[] value     = in.readAllBytes();
    }

    protected void messageSerialize(DataOutputStream out) {
        out.writeInt(requestId);
        out.write(value);
    }

    public int getRequestId() {
        return this.requestId;
    }

    public byte[] getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if ((other == null) || (this.getClass() != other.getClass()))
            return false;
        PutResponseMessage that = (PutResponseMessage) other;
        return this.requestId == that.requestId && this.value.equals(that.value);
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
        String str = "PutResponseMessage(Id= %d, Value= %s)";
        String res = String.format(str, this.requestId, Arrays.toString(this.value));
        return res;
    }
}
