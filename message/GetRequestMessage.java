import java.io.DataInputStream;
import java.io.DataOutputStream;

public class GetRequestMessage extends Message {
	private int id;
	private String key;


	public GetRequestMessage(int id, String key) {
		this.id = id;
		this.key = key;
	}


	public static GetRequestMessage messageDeserialize(DataInputStream in) {
		int id = in.readInt();
		String key = in.readUTF();
	}

	protected void messageSerialize(DataOutputStream out) {
		out.writeInt(id);
		out.writeUTF(key);
	}

	public int getId() {
		return this.id;
	}

	public String getKey() {
		return this.key;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other) return true;
		if ((other == null) || (this.getClass() != other.getClass())) return false;
		GetRequestMessage that = (GetRequestMessage) other;
		return this.id == that.id && this.key.equals(that.key);
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
	    String str = "GetRequestMessage(Id= %d, Key= %s)";
	    String res = String.format(str, this.id, this.key);
	    return res;
	}
}