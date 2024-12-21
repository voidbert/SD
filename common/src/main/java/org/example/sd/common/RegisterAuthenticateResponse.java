import java.io.DataInputStream;
import java.io.DataOutputStream;

public class RegisterAuthenticateResponse extends Message {
    private RegistrationAuthenticationStatus status;

    public RegisterAuthenticateResponse(RegistrationAuthenticationStatus status) {
        this.status = status;
    }

    public static RegisterAuthenticateResponse messageDeserialize(DataInputStream in) {
        RegistrationAuthenticationStatus status =
            RegistrationAuthenticationStatus.valueOf(in.readUTF());
    }

    protected void messageSerialize(DataOutputStream out) {
        out.writeUTF(status.toString());
    }

    public RegistrationAuthenticationStatus getStatus() {
        return this.status;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other)
            return true;
        if ((other == null) || (this.getClass() != other.getClass()))
            return false;
        RegisterAuthenticateResponse that = (RegisterAuthenticateResponse) other;
        return this.status.equals(that.status);
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
        String str = "RegisterAuthenticateResponse(Status= %s)";
        String res = String.format(str, this.status);
        return res;
    }
}
