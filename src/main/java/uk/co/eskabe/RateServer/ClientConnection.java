package uk.co.eskabe.RateServer;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

/**
 * Created by Phil on 20/04/2017.
 * This class handles the top level websocket comms.
 */
public class ClientConnection extends Thread implements RateUpdateListener {
    private ClientEventListener eventListener = null;
    private Socket connection = null;
    private SessionManager sessionManager = null;
    private WebSocketMessageHandler wsMessageHandler = null;
    protected boolean bOpen = false;
    protected String strSessionId = "";

    public ClientConnection( ClientEventListener server, Socket myConnection, SessionManager useSessionManager ) {
        eventListener = server;
        connection = myConnection;
        sessionManager = useSessionManager;
        bOpen = true;
    }

    public void run() {

        try {
            while (bOpen) {
                wsMessageHandler = new WebSocketMessageHandler(connection.getInputStream(), connection.getOutputStream());
                BufferedReader inFromClient =
                        new BufferedReader(new InputStreamReader(connection.getInputStream()));
                BufferedWriter outToClient =
                        new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
                ArrayList<String> header = new ArrayList<String>();
                String clientSentence;

                // Wait for and process the headers sent by the client.
                while ((clientSentence = inFromClient.readLine()) != null) {
                    System.out.println("Received: " + clientSentence);
                    header.add(clientSentence);
                    if (clientSentence.length() == 0) {
                        break;
                    } else if ( clientSentence.contains("permessage-deflate") ) {
                        wsMessageHandler.bCompressionRequested = true;
                    }
                }

                System.out.println("Received HTTP header with " + String.valueOf(header.size()) + " lines");

                // Now construct the response which completes the websocket handhake.
                Iterator iVals = getResponseHeader(header).listIterator();
                while (iVals.hasNext()) {
                    String headerLine = iVals.next().toString();
                    System.out.println("Sending: " + headerLine);
                    outToClient.write(headerLine, 0, headerLine.length());
                    outToClient.newLine();
                }
                outToClient.newLine();
                outToClient.flush();

                System.out.println("Waiting for first message from client...");

                while (bOpen) {

                    int msg = wsMessageHandler.waitForMessage();
                    synchronized (this) {
                        switch (msg) {
                            case 8:
                                int errorCode = wsMessageHandler.getErrorCode();
                                String errorMsg = wsMessageHandler.getTextMessage();
                                System.out.println("Close websocket reason code: " + Integer.valueOf(errorCode).toString() + ". Error msg ='" + errorMsg + "'");
                                internalOnclose();
                                bOpen = false;
                                break;
                            case 1:
                                try {
                                    String message = wsMessageHandler.getTextMessage();
                                    internalOnMessage(message);
                                } catch (Exception ex) {
                                    MessageGeneralError msgError = new MessageGeneralError(strSessionId, "Exception while processing message", ex.toString());
                                    wsMessageHandler.sendMessage(msgError.writeOut());
                                }
                                break;
                            case 2:
                                break;
                            case 9:
                                // Ping.
                                break;
                            case 10:
                                // Pong.
                                break;
                        }
                    }
                }
            }
        } catch ( Exception ex ) {
            // TBD
            System.out.println(ex.toString());
        }
    }

    public void internalOnMessage( String rxMessage ) {
        synchronized (connection) {
            // An inbound message will be one of the following:
            // 1) connect (authentication)
            // 2) subscribe (to a rate)
            // 3) unsubscribe (from a rate)
            // 4) disconnect (quitting the session)
            String strVerb = "unknown";
            try {
                MessageDecoder decoder = new MessageDecoder();
                decoder.readIn(rxMessage);
                strVerb = decoder.getVerb();

                if (decoder.isConnect()) {
                    MessageConnect connectMsg = new MessageConnect();
                    connectMsg.readIn(rxMessage);
                    System.out.println("Connect message received for user: " + connectMsg.getUsername());
                    UUID sessionIdent = sessionManager.connect(connectMsg.getUsername(), connectMsg.getPassword(), this);
                    if (sessionIdent != null) {
                        strSessionId = sessionIdent.toString();
                        connectMsg.setSessionId(strSessionId);
                        wsMessageHandler.sendMessage(connectMsg.writeOut());
                    } else {
                        MessageGeneralError errorMsg = new MessageGeneralError("", "AUTH FAILURE", "Username and/or password unknown.");
                        wsMessageHandler.sendMessage(errorMsg.writeOut());
                    }

                } else if (decoder.isSubscribe()) {
                    MessageSubscribe subscribeMsg = new MessageSubscribe();
                    subscribeMsg.readIn(rxMessage);
                    System.out.println("Subscribe message received for: " + subscribeMsg.getInstrument() + " - " + subscribeMsg.getFxPair());
                    String instr = subscribeMsg.getInstrument();
                    String fxpair = subscribeMsg.getFxPair();
                    UUID sessionId = UUID.fromString(strSessionId);
                    long result = sessionManager.subscribe(sessionId, instr, fxpair);
                    if (result == 1) {
                        wsMessageHandler.sendMessage(subscribeMsg.writeOut());
                    } else {
                        MessageGeneralError errorMsg = new MessageGeneralError(subscribeMsg.params.sessionId, "SESSION NOT RECOGNISED", "Expected " + strSessionId);
                        wsMessageHandler.sendMessage(errorMsg.writeOut());
                    }
                    sessionManager.addListener(sessionId, instr, fxpair);

                } else if (decoder.isUnsubscribe()) {
                    MessageUnsubscribe unsubscribeMsg = new MessageUnsubscribe();
                    unsubscribeMsg.readIn(rxMessage);
                    System.out.println("Unsubscribe message received.");
                    String instr = unsubscribeMsg.getInstrument();
                    String fxpair = unsubscribeMsg.getFxPair();
                    UUID sessionId = UUID.fromString(strSessionId);
                    long result = sessionManager.unsubscribe(sessionId, instr, fxpair);
                    if (result == 1) {
                        wsMessageHandler.sendMessage(unsubscribeMsg.writeOut());
                    } else {
                        MessageGeneralError errorMsg = new MessageGeneralError(unsubscribeMsg.params.sessionId, "SESSION NOT RECOGNISED", "Expected " + strSessionId);
                        wsMessageHandler.sendMessage(errorMsg.writeOut());
                    }

                } else if (decoder.isDisconnect()) {
                    MessageDisconnect disconnectMsg = new MessageDisconnect();
                    disconnectMsg.readIn(rxMessage);
                    System.out.println("Disconnect message received.");
                    UUID sessionId = UUID.fromString(strSessionId);
                    long result = sessionManager.disconnect(sessionId);
                    if (result == 1) {
                        wsMessageHandler.sendMessage(disconnectMsg.writeOut());
                    } else {
                        MessageGeneralError errorMsg = new MessageGeneralError(disconnectMsg.params.sessionid, "SESSION NOT RECOGNISED", "Expected " + strSessionId);
                        wsMessageHandler.sendMessage(errorMsg.writeOut());
                    }

                }
            } catch (ParseException pEx) {
                MessageGeneralError errorMsg = new MessageGeneralError(strSessionId, "MALFORMED PAYLOAD", pEx.toString());
                wsMessageHandler.sendMessage(errorMsg.writeOut());
            } catch (JsonSerializerException jsEx) {
                MessageGeneralError errorMsg = new MessageGeneralError(strSessionId, "MALFORMED PAYLOAD", jsEx.toString());
                wsMessageHandler.sendMessage(errorMsg.writeOut());
            }

            // Now let the listener(s) know about this message.
            eventListener.onMessage(rxMessage);
        }
    }

    public void internalOnclose() {
        try {
            connection.close();
            eventListener.onConnectionClosed(this);
        } catch (java.io.IOException ioEx) {
            System.out.println( "Error closing connection with client! " + ioEx.toString());
        }
    }

    public ArrayList<String> getResponseHeader( ArrayList<String> inboundHeader ) {

        final String handshakeGUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
        ArrayList<String> response = new ArrayList<String>();

        String websocketKey = "";
        String temp = "";
        Iterator iVals = inboundHeader.listIterator();
        while ( iVals.hasNext() ) {
            temp = (String)iVals.next();
            if ( temp.startsWith("Sec-WebSocket-Key") ) {
                websocketKey = temp.substring(temp.indexOf(":") + 2);
                break;
            }
        }

        byte digest[] = DigestUtils.sha1((websocketKey + handshakeGUID).getBytes());

        response.add( "HTTP/1.1 101 Switching Protocols" );
        response.add( "Upgrade: websocket" );
        response.add( "Connection: Upgrade" );
        response.add( "Sec-WebSocket-Accept: " + Base64.encodeBase64String(digest) );

        // Chrome wants permessage deflate but it doesn't work if enabled!
        //response.add( "Sec-WebSocket-Extensions: permessage-deflate; client_no_context_takeover; server_no_context_takeover" );

        // Safari :
        //response.add( "Sec-WebSocket-Extensions: x-webkit-deflate-frame");

        return response;
    }

    @Override
    public void rateUpdated(RateObject rateObj) {
        synchronized (connection) {
            MessageRateUpdate updateMsg = new MessageRateUpdate(strSessionId, rateObj.getInstrument(), rateObj.getFxPair(), rateObj.getPrice());
            wsMessageHandler.sendMessage(updateMsg.writeOut());
        }
    }
}
