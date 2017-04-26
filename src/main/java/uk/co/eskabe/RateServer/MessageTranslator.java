package uk.co.eskabe.RateServer;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
/**
 * Created by Phil on 19/04/2017.
 */
public class MessageTranslator {
    private String verb = null;
    private JSONObject jsonMessage = null;

    public MessageTranslator() {

    }

    public void parseInboundMessage(String rxMessage) throws ParseException {
        JSONParser parser = new JSONParser();
        jsonMessage = (JSONObject)parser.parse(rxMessage);
        verb = (String)jsonMessage.get("verb");
    }

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

    public String getUsername() {
        JSONObject parameters = (JSONObject)jsonMessage.get("params");
        return (String)parameters.get("username");
    }

    public String getPassword() {
        JSONObject parameters = (JSONObject)jsonMessage.get("params");
        return (String)parameters.get("password");
    }

    public String getSessionId() {
        JSONObject parameters = (JSONObject)jsonMessage.get("params");
        return (String)parameters.get("sessionId");
    }

    public String getInstrument() {
        JSONObject parameters = (JSONObject)jsonMessage.get("params");
        return (String)parameters.get("instrument");
    }

    public String getFxPair() {
        JSONObject parameters = (JSONObject)jsonMessage.get("params");
        return (String)parameters.get("fxPair");
    }

    public String formatConnectRequest( String username, String password ) {
        return "{ \"verb\": \"connect\", \"params\": { \"unsername\": \"" + username + "\", \"password\": \"" + password + "\"} }";
    }

    public String formatConnectResponse( String strSessionId, String strResult ) {
        return "{ \"verb\": \"connect\", \"params\": { \"sessionId\": \"" + strSessionId + "\", \"result\": \"" + strResult + "\"} }";
    }

    public String formatSubscribeRequest( String strSessionId, String instrument, String fxPair ) {
        return "{ \"verb\": \"subscribe\", \"params\": { \"sessionId\": \"" + strSessionId + "\", \"instrument\": \"" + instrument + "\", \"fxPair\": \"" + fxPair + "\"} }";
    }

    public String formatSubscribeResponse( String strSessionId, String strResult ) {
        return "{ \"verb\": \"subscribe\", \"params\": { \"sessionId\": \"" + strSessionId + "\", \"result\": \"" + strResult + "\"} }";
    }

    public String formatUnsubscribeRequest( String strSessionId, String instrument, String fxPair ) {
        return "{ \"verb\": \"unubscribe\", \"params\": { \"sessionId\": \"" + strSessionId + "\", \"instrument\": \"" + instrument + "\", \"fxPair\": \"" + fxPair + "\"} }";
    }

    public String formatUnsubscribeResponse( String strSessionId, String strResult ) {
        return "{ \"verb\": \"unsubscribe\", \"params\": { \"sessionId\": \"" + strSessionId + "\", \"result\": \"" + strResult + "\"} }";
    }

    public String formatDisconnectRequest( String strSessionId ) {
        return "{ \"verb\": \"unubscribe\", \"params\": { \"sessionId\": \"" + strSessionId + "\" } }";
    }

    public String formatDisconnectResponse( String strSessionId, String strResult ) {
        return "{ \"verb\": \"disconnect\", \"params\": { \"sessionId\": \"" + strSessionId + "\", \"result\": \"" + strResult + "\"} }";
    }

    public static String formatGeneralError(String errorHeadline, String errorDetail) {
        return "{ \"verb\": \"unknown\", \"params\": { \"error\": \"" + errorHeadline + "\", \"detail\": \"" + errorDetail + "\"} }";
    }

}
