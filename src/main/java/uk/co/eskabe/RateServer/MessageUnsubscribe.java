package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 26/04/2017.
 */
public class MessageUnsubscribe extends MessageBase {

    public class Parameters extends ParameterBase {
        String sessionId = "";
        String instrument = "";
        String fxPair = "";

        public Parameters( String useSessionId, String strInstrument, String strFxPair ) {
            sessionId = useSessionId;
            instrument = strInstrument;
            fxPair = strFxPair;
        }
    }

    public String verb = "unsubscribe";
    protected MessageUnsubscribe.Parameters params = new MessageUnsubscribe.Parameters("", "", "");

    public MessageUnsubscribe() {}

    public MessageUnsubscribe( String useSessionId, String strInstrument, String strFxPair ) {
        params.sessionId = useSessionId;
        params.instrument = strInstrument;
        params.fxPair = strFxPair;
    }

    @Override
    public String getVerb() { return verb; }

    public void setInstrument( String newValue ) {
        params.instrument = newValue;
    }

    public String getInstrument() { return params.instrument; }

    public void setFxPair( String newValue ) {
        params.fxPair = newValue;
    }

    public String getFxPair() { return params.fxPair; }
}
