package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 25/04/2017.
 */
public class MessageSubscribe extends MessageBase {

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

    public String verb = "subscribe";
    protected MessageSubscribe.Parameters params = new MessageSubscribe.Parameters("", "", "");

    public MessageSubscribe() {}

    public MessageSubscribe( String useSessionId, String strInstrument, String strFxPair ) {
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
