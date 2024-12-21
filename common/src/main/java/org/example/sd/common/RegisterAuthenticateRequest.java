import java.io.DataInputStream;
import java.io.DataOutputStream;

public class RegisterAuthenticateRequest extends Message {
    private String username;
    private String password;

    public void RegisterAuthenticateMessage(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public static RegisterAuthenticateRequest messageDeserialize(DataInputStream in) {
        String username = in.readUTF();
        String password = in.readUTF();
    }

    protected void messageSerialize(DataOutputStream out) {
        out.writeUTF(username);
        out.writeUTF(password);
    }

    public String getUsername() {
        return this.username;
    }

    public String getPassword() {
        return this.password;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if ((other == null) || (this.getClass() != other.getClass()))
            return false;
        RegisterAuthenticateRequest that = (RegisterAuthenticateRequest) other;
        return this.username.equals(that.username) && this.password.equals(that.password);
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
        String str = "RegisterAuthenticateRequest(Username= %s, Password= %s)";
        String res = String.format(str, this.username, this.password);
        return res;
    }
}
