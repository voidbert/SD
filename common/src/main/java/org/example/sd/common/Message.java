public abstract class Message {

    /**
	 *
	 * @param in
	 */
    public static Message deserialize(DataInputStream in) {
        // TODO - implement Message.deserialize
        throw new UnsupportedOperationException();
    }

    /**
	 *
	 * @param out
	 */
    public void serialize(DataOutputStream out) {
        // TODO - implement Message.serialize
        throw new UnsupportedOperationException();
    }

    /**
	 *
	 * @param out
	 */
    protected abstract void messageSerialize(DataOutputStream out);
}
