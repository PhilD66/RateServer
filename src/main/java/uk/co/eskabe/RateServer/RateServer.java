/*
 * TCPServer extended to behave as a websocket server.
 * Phil.D
 *
 * TBD:
 * Limit the number of open connections per client to prevent DoS attacks
 *
 */

package uk.co.eskabe.RateServer;

import java.io.*;
import java.net.*;
import java.rmi.server.ExportException;
import java.util.*;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.codec.binary.Base64;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

import static java.util.zip.Deflater.DEFLATED;
import javax.websocket.Encoder;

class RateServer implements ClientEventListener
{
    ArrayList<ClientConn> clientConections = null;

	protected class ClientConn extends Thread {
	    private ClientEventListener eventListener = null;
		private Socket connection = null;
        protected boolean bOpen = false;

		public ClientConn( ClientEventListener server, Socket myConnection ) {
            eventListener = server;
			connection = myConnection;
			bOpen = true;
		}

		public void run() {

		    try {
                while (bOpen) {
                    WebSocketMessageHandler wsMessageHandler = new WebSocketMessageHandler(connection.getInputStream());
                    BufferedReader inFromClient =
                            new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    BufferedWriter outToClient =
                            new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));
                    ArrayList<String> header = new ArrayList<String>();
                    String clientSentence;

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

                    Iterator iVals = getResponseHeader(header).listIterator();
                    while (iVals.hasNext()) {
                        String headerLine = iVals.next().toString();
                        System.out.println("Sending: " + headerLine);
                        outToClient.write(headerLine, 0, headerLine.length());
                        outToClient.newLine();
                    }
                    outToClient.newLine();
                    outToClient.flush();

                    System.out.println("Waiting for next message...");

                    while (bOpen) {

                        int msg = wsMessageHandler.waitForMessage();
                        switch( msg ) {
                            case 8:
                            	int errorCode = wsMessageHandler.getErrorCode();
                            	String errorMsg = wsMessageHandler.getTextMessage();
                            	System.out.println(errorMsg);
                                close();
                                bOpen = false;
                                break;
                            case 1:
                                String message = wsMessageHandler.getTextMessage();
                                eventListener.onMessage(message);
								//wsMessageHandler.sendMessage(message, connection.getOutputStream());
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

		public void close() {
			try {
				connection.close();
				eventListener.onConnectionClosed(this);
			} catch (java.io.IOException ioEx) {
				System.out.println( "Error closing connection with client! " + ioEx.toString());
			}
		}
	}

	public RateServer() {
        clientConections = new ArrayList<ClientConn>();
	}

	public static void main(String argv[]) throws Exception {

		RateServer main = new RateServer();

		main.run(6789 );
	}

	public void run( int port ) throws Exception {

		ServerSocket welcomeSocket = new ServerSocket(port);

		while( true ) {
			Socket connectionSocket = welcomeSocket.accept();
			connectionSocket.setTcpNoDelay(true);

			ClientConn newClient	= new ClientConn(this, connectionSocket);
			clientConections.add(newClient);
			newClient.start();
			System.out.println( "There are " + String.valueOf(clientConections.size()) + " clients connected.");
		}
	}

    /****
     * Implementation of ClientEventListener interface onMessage method.
     * @param rxMessage - The message received from the client.
     */
	public void onMessage( String rxMessage ) {
        // An inbound message will be one of the following:
        // 1) connect (authentication)
        // 2) subscribe (to a rate)
        // 3) unsubscribe (from a rate)
        // 4) disconnect (quitting the session)
        MessageTranslator translator = new MessageTranslator(rxMessage);
        if (translator.isConnect()) {
            System.out.println("Connect message received.");

        } else if (translator.isSubscribe()) {
            System.out.println("Subscribe message received.");

        } else if (translator.isUnsubscribe()) {
            System.out.println("Unsubscribe message received.");

        } else if (translator.isDisconnect()) {
            System.out.println("Disconnect message received.");

        }
    }

    /***
     * Implementation of ClientEventListener interface onConnectionClosed method.
     * @param obj
     */
	public void onConnectionClosed( Object obj ) {
        clientConections.remove(obj);
        System.out.println( "There are " + String.valueOf(clientConections.size()) + " clients connected.");
    }

	public static ArrayList<String> getResponseHeader( ArrayList<String> inboundHeader ) {

		String handshakeGUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
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