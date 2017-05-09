package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 26/04/2017.
 */
public class MessageGeneralError extends MessageBase {

    public class Parameters extends ParameterBase {
        public String sessionid = "";
        public String error = "";
        public String detail = "";
    }

    public String verb = "error";
    public MessageGeneralError.Parameters params = new MessageGeneralError.Parameters();

    public MessageGeneralError() { ; }

    public MessageGeneralError( String strSessionId, String strError, String strDetail ) {
        params.sessionid = strSessionId;
        params.error = strError;
        params.detail = strDetail;
    }

    @Override
    public String getVerb() { return verb; }
}
