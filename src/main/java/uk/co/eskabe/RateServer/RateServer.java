/*
 * RateServer is the MAIN class to run the websocket based RateServer.
 * This class opens a socket and when it gets a connection from a client it
 * hands off to ClientConnection.
 *
 * Phil.D
 *
 * TBD:
 * Limit the number of open connections per client to prevent DoS attacks
 *
 */

package uk.co.eskabe.RateServer;

import java.net.*;
import java.util.ArrayList;

class RateServer implements ClientEventListener
{
    private ArrayList<ClientConnection> clientConections = null;

	public RateServer() {
        clientConections = new ArrayList<ClientConnection>();
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

            ClientConnection newClient	= new ClientConnection(this, connectionSocket);
			clientConections.add(newClient);
			newClient.start();
			System.out.println( "There are " + String.valueOf(clientConections.size()) + " clients connected.");
		}
	}

    /****
     * Empty implementation of ClientEventListener interface onMessage method.
     * @param rxMessage - The message received from the client.
     */
	public void onMessage( String rxMessage ) {
	    // No interest in the content of the messages.
        // This class only cares about socket-level and websocket connection and status.
    }

    /***
     * Implementation of ClientEventListener interface onConnectionClosed method.
     * This method is called when the remote client closes the socket or by ClientConnection
     * if there is a protocol error or horrible exception.
     * @param listener - The ClientEventListener object that needs to be removed from the list.
     */
	public void onConnectionClosed(ClientEventListener listener) {
        clientConections.remove(listener);
        System.out.println( "There are " + String.valueOf(clientConections.size()) + " clients connected.");
    }


}