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

class RateClient implements ClientEventListener
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
                    String clientSentence;
                    BufferedReader inFromClient =
                            new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    BufferedWriter outToClient =
                            new BufferedWriter(new OutputStreamWriter(connection.getOutputStream()));

                    ArrayList<String> header = new ArrayList<String>();

                    while ((clientSentence = inFromClient.readLine()) != null) {
                        System.out.println("Received: " + clientSentence);
                        header.add(clientSentence);
                        if (clientSentence.length() == 0) {
                            break;
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

                    System.out.println("Waiting for bytes...");
                    InputStream rawIn = connection.getInputStream();

                    while (bOpen) {
                        int chunk = 0;
                        byte buffer[] = new byte[64];  // Create a buffer that's large enough as a temporary store.
                        byte messageBuffer[] = null;
                        int lengthCode = -1;
                        int frameHeader = 2;
                        int messageLength = 0;
                        boolean bCompressed = false;
                        boolean bMask = false;
                        int payloadSize = 0;
                        byte opcode;

                        // Read only 2 bytes which is the minimum raw header
                        chunk = rawIn.read(buffer, 0, 2);
                        if (chunk == 2) {
                            lengthCode = (0x7F & buffer[1]);
                            opcode = (byte) (0x0F & buffer[0]);
                            bCompressed = (0x40 & buffer[0]) == 0x40;
                            switch( opcode ) {
                                case 0x0:
                                    // Continuation frame. TBD!
                                    break;
                                case 0x8:
                                    //close();
                                    bOpen = false;
                                    break;
                                case 0x9:
                                    // Ping. TBD!
                                    break;
                                case 0xA:
                                    // Pong. TBD.
                                    break;

                            }
                            bMask = ((0x80 & buffer[1]) != 0);
                        } else {
                            // throw new Exception("Unhandled scenario! Insufficient bytes read to decode frame header.");
                            System.out.println("Unexpected byte count. Closing socket.");
                            close();
                            break;
                        }

                        if ( lengthCode == 0 ) {
                            payloadSize = 0;
                            break;
                        } else if (lengthCode < 126) {
                            payloadSize = lengthCode;
                        } else {
                            if (lengthCode == 126) {
                                chunk = rawIn.read(buffer, 2, 2);
                                payloadSize = ((0xFF & buffer[2]) << 8) + (0xFF & buffer[3]) + 2; // Adding 2 bytes minimum frame start.
                                frameHeader = 4;
                            } else if (lengthCode == 127) {
                                close();
                                throw new Exception("Message size too large for this implementation!");
                            }
                        }

                        System.out.println(String.format("Expect %d bytes of payload", payloadSize));

                        if ( payloadSize > 0 ) {

                            if (bMask) {
                                chunk = rawIn.read(buffer, frameHeader, 4);
                                frameHeader += 4; // Add mask bytes
                            }

                            // Now we know what length of message to expect we can allocate a buffer for it, allowing for 2:1 compression.
                            if (messageBuffer == null) {
                                System.out.println("Creating buffer for message of total length = " + String.valueOf(payloadSize));
                                // The below won't work if the payload is > 2^31. Assume unlikely for now.
                                messageBuffer = new byte[payloadSize];
                            }

                            // read data - note: may not read fully (or evenly), read from stream until len==0
                            int len, offset = 0;
                            while ((len = rawIn.read(messageBuffer, offset, messageBuffer.length - offset)) > 0) {
                                offset += len;
                            }

                            if (bMask) {
                                int oct = 0;
                                for (int mbyte = 0; mbyte < payloadSize; mbyte++) {
                                    messageBuffer[mbyte] = (byte) (messageBuffer[mbyte] ^ buffer[frameHeader - 4 + (oct % 4)]);
                                    oct++;
                                }
                            }

                            byte[] uncompressedData = null;
                            int resultLength = 0;
                            // Only works with Chrome which seems to insist on compression.
                            if (bCompressed) {
                                Inflater decompresser = new Inflater(true);
                                decompresser.setInput(messageBuffer, 0, (int) payloadSize);
                                uncompressedData = new byte[2 * payloadSize]; // Assume maximum of 2:1 compression.
                                resultLength = decompresser.inflate(uncompressedData);
                                decompresser.finished();
                            } else {
                                resultLength = payloadSize;
                                uncompressedData = new byte[payloadSize]; // Assume maximum of 2:1 compression.
                                for (int copy = 0; copy < payloadSize; copy++) {
                                    uncompressedData[copy] = messageBuffer[copy];
                                }
                            }

                            offset = 0;
                            if ( bOpen == false ) {
                                // Close request being processed.
                                int closeCode = ((0xFF & uncompressedData[0]) << 8) + (0xFF & uncompressedData[1]);
                                offset = 2;
                            }
                            String message = new String(uncompressedData, offset, resultLength - offset, "UTF-8");
                            System.out.println(message);

                            processMessage2(message, connection.getOutputStream());
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

	public RateClient() {

	}

	public static void main(String argv[]) throws Exception {

		RateClient main = new RateClient();

		main.run(6789 );
	}

	public void run( int port ) throws Exception {

		Socket   clientSocket = new Socket("127.0.0.1", 6789);

        BufferedWriter outToClient =
                new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        BufferedReader inFromClient =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        outToClient.write("GET / HTTP/1.1");
        outToClient.newLine();
        outToClient.write("Host: localhost:6789");
        outToClient.newLine();
        outToClient.write("Connection: Upgrade");
        outToClient.newLine();
        outToClient.write("Pragma: no-cache");
        outToClient.newLine();
        outToClient.write("Cache-Control: no-cache");
        outToClient.newLine();
        outToClient.write("Upgrade: websocket");
        outToClient.newLine();
        outToClient.write("Origin: http://localhost:8080");
        outToClient.newLine();
        outToClient.write("Sec-WebSocket-Version: 13");
        outToClient.newLine();
        outToClient.write("User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36");
        outToClient.newLine();
        outToClient.write("Accept-Encoding: gzip, deflate");
        outToClient.newLine();
        outToClient.write("Accept-Language: en-US,en;q=0.8");
        outToClient.newLine();
        outToClient.write("Sec-WebSocket-Key: L1Ii5SGijbGmWp9hWsebOg==");
        outToClient.newLine();
        outToClient.write("Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits");
        outToClient.newLine();
        outToClient.newLine();
        outToClient.flush();

        String clientSentence;
        while ((clientSentence = inFromClient.readLine()) != null) {
            System.out.println("Received: " + clientSentence);
            if (clientSentence.length() == 0) {
                break;
            }
        }

        // Send a message to the server.
        processMessage2("Test message from Java client!", clientSocket.getOutputStream());

        // Then wait for it to send something back.
        byte    buffer[] = new byte[2048];
        int     readCount = 0;
        InputStream iStr = clientSocket.getInputStream();

        String response = extractMessage(iStr);
        System.out.println("Received: " + response);
	}

	public void onMessage( String rxMessage ) {

    }

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
		// "chat" causes the client to kill the connection!
		//response.add( "Sec-WebSocket-Protocol: chat" );
        // response.add( "Accept-Encoding: gzip, deflate" );

        // Chrome needs this...
		///response.add( "Sec-WebSocket-Extensions: permessage-deflate; client_no_context_takeover; server_no_context_takeover" );
        // response.add( "Sec-WebSocket-Extensions: permessage-deflate" );
        // Safari :
        response.add( "Sec-WebSocket-Extensions: x-webkit-deflate-frame");

        //response.add( "Access-Control-Allow-Origin: http://localhost:8080" );

		return response;

	}

	public void processMessage(String rxMessage, OutputStream streamOut) {
	    // Do something with the message here...

        System.out.println("Message back to client is: " + rxMessage);

        // And then compress the response and send it out.
	    Deflater compressor = new Deflater(Deflater.DEFLATED);
        try {
            int headerLength = 2;
            byte unzippedMsg[] = rxMessage.getBytes("UTF-8");
            //compressor.setInput(unzippedMsg);
            //compressor.finish();
            byte zippedMsg[] = new byte[rxMessage.length() * 2 + headerLength];
            int toCompressLength = unzippedMsg.length;
            for ( int copy = 0; copy < toCompressLength; copy++ ) {
                zippedMsg[copy + headerLength] = unzippedMsg[copy];
            }
            int compLength = toCompressLength;
            //int compLength = compressor.deflate(zippedMsg, headerLength, zippedMsg.length - headerLength);
            //compressor.end();

            zippedMsg[0] = (byte)0x81; // FIN bit plus opcode for TEXT MESSAGE
            zippedMsg[1] = (byte)((byte)0x00 | (byte)compLength); // No mask on return data.

            streamOut.write(zippedMsg, 0, compLength + headerLength);

            // Not necessary: streamOut.flush();

        } catch ( IOException ioEx ) {
	        // TBD
            System.out.println("IOException: " + ioEx.toString());
        } catch ( Exception ex ) {
            // TBD
            System.out.println("IOException: " + ex.toString());
        }
    }

    public void processMessage2(String rxMessage, OutputStream streamOut) {

        System.out.println("Message back to client is: " + rxMessage);

        // And then compress the response and send it out.
        Deflater compressor = new Deflater(Deflater.DEFLATED, true);
        try {
            int headerLength = 2;
            byte unzippedMsg[] = rxMessage.getBytes("UTF-8");
            compressor.setInput(unzippedMsg);
            compressor.finish();
            byte zippedMsg[] = new byte[2048];  // Nasty constant but will have to do for now.
            int toCompressLength = unzippedMsg.length;
            int compLength = compressor.deflate(zippedMsg, headerLength, zippedMsg.length - headerLength);
            compressor.end();

            zippedMsg[0] = (byte)0xC1; // FIN bit, compression plus opcode for TEXT MESSAGE
            zippedMsg[1] = (byte)((byte)0x00 | (byte)compLength); // No mask on return data.

            streamOut.write(zippedMsg, 0, compLength + headerLength);

        } catch ( IOException ioEx ) {
            // TBD
            System.out.println("IOException: " + ioEx.toString());
        } catch ( Exception ex ) {
            // TBD
            System.out.println("IOException: " + ex.toString());
        }
    }

    public void processMessage3(String rxMessage, OutputStream streamOut) {
        // Do something with the message here...

        System.out.println("Message back to client is: " + rxMessage);

        // And then compress the response and send it out.
        try {
            int headerLength = 2;
            byte unzippedMsg[] = rxMessage.getBytes("UTF-8");
            int toCompressLength = unzippedMsg.length;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
            gzipOut.write(unzippedMsg, 0, toCompressLength);
            gzipOut.close();
            byte[] payload = baos.toByteArray();

            byte header[] = new byte[32];
            header[0] = (byte)0xC2; // FIN bit plus opcode for TEXT MESSAGE
            header[1] = (byte)((byte)0x00 | (byte)payload.length); // No mask on return data.

            streamOut.write(header, 0, 2);
            streamOut.write(payload);

        } catch ( IOException ioEx ) {
            // TBD
            System.out.println("IOException: " + ioEx.toString());
        } catch ( Exception ex ) {
            // TBD
            System.out.println("IOException: " + ex.toString());
        }
    }

    protected String extractMessage(InputStream rawIn) {
        boolean bOpen = true;
        String message = "";

        while (bOpen) {
            int chunk = 0;
            byte buffer[] = new byte[64];  // Create a buffer that's large enough as a temporary store.
            byte messageBuffer[] = null;
            int lengthCode = -1;
            int frameHeader = 2;
            int messageLength = 0;
            boolean bCompressed = false;
            boolean bMask = false;
            int payloadSize = 0;
            byte opcode;

            // Read only 2 bytes which is the minimum raw header
            try {
                chunk = rawIn.read(buffer, 0, 2);
            } catch (IOException ioEx) {
                return "";
            }
            if (chunk == 2) {
                lengthCode = (0x7F & buffer[1]);
                opcode = (byte) (0x0F & buffer[0]);
                bCompressed = (0x40 & buffer[0]) == 0x40;
                switch (opcode) {
                    case 0x0:
                        // Continuation frame. TBD!
                        break;
                    case 0x8:
                        //close();
                        bOpen = false;
                        break;
                    case 0x9:
                        // Ping. TBD!
                        break;
                    case 0xA:
                        // Pong. TBD.
                        break;

                }
                bMask = ((0x80 & buffer[1]) != 0);
            } else {
                // throw new Exception("Unhandled scenario! Insufficient bytes read to decode frame header.");
                System.out.println("Unexpected byte count. Closing socket.");
                // close(); ****************  Need to call this.
                break;
            }

            if (lengthCode == 0) {
                payloadSize = 0;
                break;
            } else if (lengthCode < 126) {
                payloadSize = lengthCode;
            } else {
                if (lengthCode == 126) {
                    try {
                        chunk = rawIn.read(buffer, 2, 2);
                    } catch (IOException ioEx) {
                        return "";
                    }
                    payloadSize = ((0xFF & buffer[2]) << 8) + (0xFF & buffer[3]) + 2; // Adding 2 bytes minimum frame start.
                    frameHeader = 4;
                } else if (lengthCode == 127) {
                    // close(); ****************  Need to call this.
                    // throw new Exception("Message size too large for this implementation!");
                    return "";
                }
            }

            System.out.println(String.format("Expect %d bytes of payload", payloadSize));

            if (payloadSize > 0) {

                try {
                    if (bMask) {
                        chunk = rawIn.read(buffer, frameHeader, 4);
                        frameHeader += 4; // Add mask bytes
                    }
                } catch (IOException ioEx) {
                    return "";
                }

                // Now we know what length of message to expect we can allocate a buffer for it, allowing for 2:1 compression.
                if (messageBuffer == null) {
                    System.out.println("Creating buffer for message of total length = " + String.valueOf(payloadSize));
                    // The below won't work if the payload is > 2^31. Assume unlikely for now.
                    messageBuffer = new byte[payloadSize];
                }

                // read data - note: may not read fully (or evenly), read from stream until len==0
                int len, offset = 0;
                try {
                    while ((len = rawIn.read(messageBuffer, offset, messageBuffer.length - offset)) > 0) {
                        offset += len;
                    }
                } catch (IOException ioEx) {
                    return "";
                }

                if (bMask) {
                    int oct = 0;
                    for (int mbyte = 0; mbyte < payloadSize; mbyte++) {
                        messageBuffer[mbyte] = (byte) (messageBuffer[mbyte] ^ buffer[frameHeader - 4 + (oct % 4)]);
                        oct++;
                    }
                }

                try {
                    byte[] uncompressedData = null;
                    int resultLength = 0;
                    // Only works with Chrome which seems to insist on compression.
                    if (bCompressed) {
                        Inflater decompresser = new Inflater(true);
                        decompresser.setInput(messageBuffer, 0, (int) payloadSize);
                        uncompressedData = new byte[2 * payloadSize]; // Assume maximum of 2:1 compression.
                        resultLength = decompresser.inflate(uncompressedData);
                        decompresser.finished();
                    } else {
                        resultLength = payloadSize;
                        uncompressedData = new byte[payloadSize]; // Assume maximum of 2:1 compression.
                        for (int copy = 0; copy < payloadSize; copy++) {
                            uncompressedData[copy] = messageBuffer[copy];
                        }
                    }

                    offset = 0;
                    if (bOpen == false) {
                        // Close request being processed.
                        int closeCode = ((0xFF & uncompressedData[0]) << 8) + (0xFF & uncompressedData[1]);
                        offset = 2;
                    }
                    message = new String(uncompressedData, offset, resultLength - offset, "UTF-8");
                    System.out.println(message);
                } catch (Throwable thr) {
                    System.out.println("Exception: " + thr.toString());
                    return "";
                }
            }
        }

        return message;
    }
}