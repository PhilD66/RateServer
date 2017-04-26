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
public class ClientConnection extends Thread {
    private ClientEventListener eventListener = null;
    private Socket connection = null;
    private SessionManager sessionManager = SessionManager.getInstance();
    private WebSocketMessageHandler wsMessageHandler = null;
    protected boolean bOpen = false;

    public ClientConnection( ClientEventListener server, Socket myConnection ) {
        eventListener = server;
        connection = myConnection;
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
                    switch( msg ) {
                        case 8:
                            int errorCode = wsMessageHandler.getErrorCode();
                            String errorMsg = wsMessageHandler.getTextMessage();
                            System.out.println(errorMsg);
                            internalOnclose();
                            bOpen = false;
                            break;
                        case 1:
                            try {
                                String message = wsMessageHandler.getTextMessage();
                                internalOnMessage(message);
                            } catch ( Exception ex ) {
                                wsMessageHandler.sendMessage(MessageTranslator.formatGeneralError("ERROR", ex.toString()));
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
        } catch ( Exception ex ) {
            // TBD
            System.out.println(ex.toString());
        }
    }

    public void internalOnMessage( String rxMessage ) {
        // An inbound message will be one of the following:
        // 1) connect (authentication)
        // 2) subscribe (to a rate)
        // 3) unsubscribe (from a rate)
        // 4) disconnect (quitting the session)
        try {
            MessageTranslator translator = new MessageTranslator();
            translator.parseInboundMessage(rxMessage);

            if (translator.isConnect()) {
                System.out.println("Connect message received for user: " + translator.getUsername());
                String username = translator.getUsername();
                String password = translator.getPassword();
                UUID sessionIdent = sessionManager.connect(username, password);
                if ( sessionIdent != null ) {
                    String strSessionId = sessionIdent.toString();
                    wsMessageHandler.sendMessage(translator.formatConnectResponse(strSessionId, "SUCCESS"));
                } else {
                    wsMessageHandler.sendMessage(translator.formatConnectResponse("", "AUTH FAILURE"));
                }

            } else if (translator.isSubscribe()) {
                System.out.println("Subscribe message received for: " + translator.getInstrument() + " - " + translator.getFxPair());
                String strSessionId = translator.getSessionId();
                String instr = translator.getInstrument();
                String fxpair = translator.getFxPair();
                UUID sessionId = UUID.fromString(strSessionId);
                long result = sessionManager.subscribe(sessionId, instr, fxpair);
                if ( result == 1 ) {
                    wsMessageHandler.sendMessage(translator.formatSubscribeResponse(strSessionId, "SUCCESS"));
                } else {
                    wsMessageHandler.sendMessage(translator.formatSubscribeResponse(strSessionId, "SESSION NOT RECOGNISED"));
                }

            } else if (translator.isUnsubscribe()) {
                System.out.println("Unsubscribe message received.");
                String strSessionId = translator.getSessionId();
                String instr = translator.getInstrument();
                String fxpair = translator.getFxPair();
                UUID sessionId = UUID.fromString(strSessionId);
                long result = sessionManager.unsubscribe(sessionId, instr, fxpair);
                if ( result == 1 ) {
                    wsMessageHandler.sendMessage(translator.formatUnsubscribeResponse(strSessionId, "SUCCESS"));
                } else {
                    wsMessageHandler.sendMessage(translator.formatUnsubscribeResponse(strSessionId, "SESSION NOT RECOGNISED"));
                }

            } else if (translator.isDisconnect()) {
                System.out.println("Disconnect message received.");
                String strSessionId = translator.getSessionId();
                UUID sessionId = UUID.fromString(strSessionId);
                long result = sessionManager.disconnect(sessionId);
                if ( result == 1 ) {
                    wsMessageHandler.sendMessage(translator.formatDisconnectResponse(strSessionId, "SUCCESS"));
                } else {
                    wsMessageHandler.sendMessage(translator.formatDisconnectResponse(strSessionId, "SESSION NOT RECOGNISED"));
                }

            }
        } catch (ParseException pEx) {
            wsMessageHandler.sendMessage(MessageTranslator.formatGeneralError("PAYLOAD MALFORMED", pEx.toString()));
        }

        // Now let the listener(s) know about this message.
        eventListener.onMessage(rxMessage);
    }

    public void internalOnclose() {
        try {
            connection.close();
            eventListener.onConnectionClosed((ClientEventListener)eventListener);
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
}
