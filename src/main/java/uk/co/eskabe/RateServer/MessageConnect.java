package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 24/04/2017.
 */
public class MessageConnect extends MessageBase {

    public class Parameters extends ParameterBase {
        String sessionid = "";
        String username = "";
        String password = "";

        public Parameters( String strUsername, String strPassword ) {
            username = strUsername;
            password = strPassword;
        }
    }

    public String verb = "connect";
    protected MessageConnect.Parameters params = new MessageConnect.Parameters("", "");

    public MessageConnect() {}

    public MessageConnect( String strUsername, String strPassword ) {
        params.username = strUsername;
        params.password = strPassword;
    }

    @Override
    public String getVerb() { return verb; }

    public void setUsername(String newValue ) {
        params.username = newValue;
    }

    public String getUsername() { return params.username; }

    public void setPassword( String newValue ) {
        params.password = newValue;
    }

    public String getPassword() { return params.password; }

    public void setSessionId( String strSessionId ) {
        params.sessionid = strSessionId;
    }

    public String getSessionId() { return params.sessionid; }
}
