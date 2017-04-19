package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 19/04/2017.
 */
public class MessageTranslator {
    String message;
    public MessageTranslator(String rxMessage) {
        message = rxMessage;
    }

    public boolean isConnect() {
        return message.compareToIgnoreCase("connect") == 0;
    }

    public boolean isSubscribe() {
        return message.compareToIgnoreCase("subscribe") == 0;
    }

    public boolean isUnsubscribe() {
        return message.compareToIgnoreCase("unsubscribe") == 0;
    }

    public boolean isDisconnect() {
        return message.compareToIgnoreCase("disconnect") == 0;
    }
}
