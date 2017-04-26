package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 26/04/2017.
 */
public class MessageGeneralError extends JsonSerializerBase {
    public class Parameters extends JsonSerializerBase {
        public String error = "";
        public String detail = "";
    }

    protected String verb = "unknown";
    public MessageGeneralError.Parameters params = new MessageGeneralError.Parameters();

    public MessageGeneralError( ) { ; }

    public MessageGeneralError( String strError, String strDetail ) {
        params.error = strError;
        params.detail = strDetail;
    }
}
