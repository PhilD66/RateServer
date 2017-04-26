package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 24/04/2017.
 */
public class MessageConnect extends JsonSerializerBase {

    public class Parameters extends JsonSerializerBase {
        String username = "";
        String password = "";

        public Parameters( String strUsername, String strPassword ) {
            username = strUsername;
            password = strPassword;
        }
    }

    protected String verb = "connect";
    protected MessageConnect.Parameters params = new MessageConnect.Parameters("", "");

    public MessageConnect( String strUsername, String strPassword ) {
        params.username = strUsername;
        params.password = strPassword;
    }

    public void setUsername( String newValue ) {
        params.username = newValue;
    }

    public String getUsername() { return params.username; }

    public void setPassword( String newValue ) {
        params.password = newValue;
    }

    public String getPassword() { return params.password; }
}
