package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 02/05/2017.
 */
public class MessageDisconnect extends MessageBase {

    public class Parameters extends ParameterBase {
        String sessionid = "";

        public Parameters( String strSessionId ) {
            sessionid = strSessionId;
        }
    }

    public String verb = "disconnect";

    public MessageDisconnect.Parameters params = new MessageDisconnect.Parameters("");

    public MessageDisconnect( ) {}

    public MessageDisconnect( String strSessionId ) {
        params.sessionid = strSessionId;
    }

    public String getVerb() { return verb; }
}
