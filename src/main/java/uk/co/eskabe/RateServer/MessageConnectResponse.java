package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 25/04/2017.
 */
public class MessageConnectResponse extends JsonSerializerBase {
    public class Parameters extends JsonSerializerBase {
        String sessionId = "";
        String result = "";

        public Parameters( String useSessionId, String strResult ) {
            sessionId = useSessionId;
            result = strResult;
        }
    }

    protected String verb = "connect";

    protected MessageConnectResponse.Parameters params = new MessageConnectResponse.Parameters("", "");

}
