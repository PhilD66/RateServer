package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 26/04/2017.
 */
public abstract class MessageBase extends JsonSerializerBase {

    public MessageBase() { ; }

    abstract public String getVerb();
}
