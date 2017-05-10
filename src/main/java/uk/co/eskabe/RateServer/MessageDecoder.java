package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 02/05/2017.
 */
public class MessageDecoder extends MessageBase {

    // The verb is assumed to be unknown until it's serialised into the object from an inbound stream.
    public String verb = "unknown";

    public String getVerb() { return verb; }

    public boolean isConnect() {
        return (verb != null) && (verb.compareToIgnoreCase("connect") == 0);
    }

    public boolean isSubscribe() {
        return (verb != null) && (verb.compareToIgnoreCase("subscribe") == 0);
    }

    public boolean isUnsubscribe() {
        return (verb != null) && (verb.compareToIgnoreCase("unsubscribe") == 0);
    }

    public boolean isDisconnect() {
        return (verb != null) && (verb.compareToIgnoreCase("disconnect") == 0);
    }

    public boolean isRateUpdate() {
        return (verb != null) && (verb.compareToIgnoreCase("update") == 0);
    }

    public boolean isError() {
        return (verb != null) && (verb.compareToIgnoreCase("error") == 0);
    }
}
