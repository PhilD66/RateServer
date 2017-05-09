package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 03/05/2017.
 */
public class MessageRateUpdate extends MessageBase {
    public class Parameters extends ParameterBase {
        String sessionId = "";
        String instrument = "";
        String fxPair = "";
        double price = 0.0;

        public Parameters() {}

        public Parameters( String useSessionId, String strInstrument, String strFxPair, double setPrice ) {
            sessionId = useSessionId;
            instrument = strInstrument;
            fxPair = strFxPair;
            price = setPrice;
        }

        public void setParameters( String useSessionId, String strInstrument, String strFxPair, double setPrice ) {
            sessionId = useSessionId;
            instrument = strInstrument;
            fxPair = strFxPair;
            price = setPrice;
        }
    }

    public String verb = "update";

    protected MessageRateUpdate.Parameters params = new MessageRateUpdate.Parameters();

    public MessageRateUpdate() {}

    public MessageRateUpdate( String useSessionId, String strInstrument, String strFxPair, double setPrice ) {
        params.setParameters(useSessionId, strInstrument, strFxPair, setPrice);
    }

    public String getVerb() { return verb; }
}
