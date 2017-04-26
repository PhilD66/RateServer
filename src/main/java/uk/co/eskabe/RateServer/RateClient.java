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
import java.util.*;

import java.util.zip.Deflater;
import java.util.zip.Inflater;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

public class RateClient implements ClientEventListener
{
    Socket clientSocket = null;
    static final int COMPRESSION_NONE = 0;
    static final int COMPRESSION_DEFLATE = 1;
    static final int COMPRESSION_GZIP = 2;
    protected int compression = COMPRESSION_NONE;
    protected boolean bMask = true;

	public RateClient() throws IOException {
        clientSocket = new Socket("127.0.0.1", 6789);
    }

	public static void main(String argv[]) throws Exception {

		RateClient main = new RateClient();

		main.run(6789 );
	}

	public void run( int port ) throws Exception {

	    try {

            doHeaderExchange();

            // Send a connect message to the server.
            MessageConnect msgConnect = new MessageConnect("sauron", "of-mordor");
            String strMessage = msgConnect.writeOut();
            sendMessage(strMessage, clientSocket.getOutputStream());

            // Then wait for it to send something back.
            InputStream iStr = clientSocket.getInputStream();

            String response = extractMessage(iStr);
            System.out.println("Received: " + response);
            MessageConnectResponse connectResponse = new MessageConnectResponse();
            try {
                connectResponse.readIn(response);

                // Now subscribe to an FX rate stream...
                MessageSubscribe msgSubcribe = new MessageSubscribe( connectResponse.params.sessionId,"SPOT", "EURGBP");
                strMessage = msgSubcribe.writeOut();
                sendMessage(strMessage, clientSocket.getOutputStream());

                response = extractMessage(iStr);
                System.out.println("Received: " + response);

            } catch ( JsonSerializerException jsEx ) {
                MessageGeneralError errorResponse = new MessageGeneralError();
                errorResponse.readIn(response);
                System.out.println("Server signals error! Error: " + errorResponse.params.error + " -> " + errorResponse.params.detail);
            }

        } catch (IOException ioEx ) {
	        System.out.println(ioEx.toString());
        }
	}

	public void doHeaderExchange() throws IOException {
        BufferedWriter outToClient =
                new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
        BufferedReader inFromClient =
                new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

        // Assemble the header...
        ArrayList<String> theHeader = new ArrayList<String>();

        theHeader.add("GET / HTTP/1.1");
        theHeader.add("Host: localhost:6789");
        theHeader.add("Connection: Upgrade");
        theHeader.add("Pragma: no-cache");
        theHeader.add("Cache-Control: no-cache");
        theHeader.add("Upgrade: websocket");
        theHeader.add("Origin: http://localhost:8080");
        theHeader.add("Sec-WebSocket-Version: 13");
        theHeader.add("User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_9_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/57.0.2987.133 Safari/537.36");
        theHeader.add("Accept-Encoding: gzip, deflate");
        theHeader.add("Accept-Language: en-US,en;q=0.8");
        theHeader.add("Sec-WebSocket-Key: L1Ii5SGijbGmWp9hWsebOg==");
        theHeader.add("Sec-WebSocket-Extensions: permessage-deflate; client_max_window_bits");

        // Now spit the header out to the websocket server.
        Iterator    iHeaders = theHeader.iterator();
        while ( iHeaders.hasNext() ) {
            outToClient.write((String)iHeaders.next());
            outToClient.newLine();
        }
        outToClient.newLine();
        outToClient.flush();

        String clientSentence;
        while ((clientSentence = inFromClient.readLine()) != null) {
            System.out.println("Received: " + clientSentence);
            if (clientSentence.length() == 0) {
                break;
            }
        }

    }

	public void onMessage( String rxMessage ) {

    }

	public void onConnectionClosed(ClientEventListener listener) {
        //clientConections.remove(listener);
        //System.out.println( "There are " + String.valueOf(clientConections.size()) + " clients connected.");
    }

    public void sendMessage(String rxMessage, OutputStream streamOut) {
        // Do something with the message here...

        System.out.println("Message to server is: " + rxMessage);

        // Turn the raw message into the payload (which may or may not be zipped).
        try {
            int maxHeaderLength = 10;
            byte rawMsg[] = rxMessage.getBytes("UTF-8");

            //  Allow space for zipped message bigger than raw (when short messages)
            byte payload[] = new byte[(rawMsg.length * 2) + maxHeaderLength];
            int toCompressLength = rawMsg.length;
            int actualPayloadLength;

            switch( compression ) {
                case COMPRESSION_DEFLATE:
                    // Fill the payload using Deflate compression.
                    actualPayloadLength = deflateMessageToPayload( rawMsg, payload, maxHeaderLength, rawMsg.length);
                    break;
                case COMPRESSION_GZIP:
                    // Fill the payload using GZIP compression.
                    actualPayloadLength = gzipMessageToPayload( rawMsg, payload, maxHeaderLength, rawMsg.length);
                    break;
                default:
                    // Assume no compression.
                    actualPayloadLength = copyMessageToPayload( rawMsg, payload, maxHeaderLength, rawMsg.length);
                    break;
            }

            int headerLength = 0;
            byte maskByteCode = 0;
            if ( bMask ) {
                headerLength = 4;
                maskByteCode = (byte)0x80;
            }

            if ( actualPayloadLength < 126 ) {
                headerLength += 2;
                if ( compression != COMPRESSION_NONE ) {
                    payload[maxHeaderLength - headerLength] = (byte)0xC1; // FIN bit plus compression bit plus opcode for TEXT MESSAGE
                } else {
                    payload[maxHeaderLength - headerLength] = (byte)0x81; // FIN bit plus opcode for TEXT MESSAGE
                }
                payload[maxHeaderLength - headerLength + 1] = (byte)(maskByteCode | (byte)actualPayloadLength);
            } else if ( actualPayloadLength < 65536 ) {
                headerLength += 4;
                if ( compression != COMPRESSION_NONE ) {
                    payload[maxHeaderLength - headerLength] = (byte)0xC1; // FIN bit plus compression bit plus opcode for TEXT MESSAGE
                } else {
                    payload[maxHeaderLength - headerLength] = (byte)0x81; // FIN bit plus opcode for TEXT MESSAGE
                }
                payload[maxHeaderLength - headerLength + 1] = (byte)(maskByteCode | (byte)126); // 126 means 2 extra bytes of data.
                payload[maxHeaderLength - headerLength + 2] = (byte)(actualPayloadLength >> 8);
                payload[maxHeaderLength - headerLength + 3] = (byte)actualPayloadLength;
            } else {
                throw new Exception("Uncoded handling for large messages!");
            }

            if ( bMask ) {
                int random = (int)(2000000000 * Math.random());
                byte mask[] = new byte[4];
                for (int maskbyte = 0; maskbyte < 4; maskbyte++) {
                    mask[maskbyte] = (byte)(random >> (8 * (3 - maskbyte)));
                    payload[maxHeaderLength - 4 + maskbyte] = mask[maskbyte];
                }
                maskMessagePayload(mask, payload, maxHeaderLength, actualPayloadLength);
            }

            streamOut.write(payload, maxHeaderLength - headerLength, actualPayloadLength + headerLength);

            // Not necessary: streamOut.flush();

        } catch ( IOException ioEx ) {
            // TBD
            System.out.println("IOException: " + ioEx.toString());
        } catch ( Exception ex ) {
            // TBD
            System.out.println("IOException: " + ex.toString());
        }
    }

    public int copyMessageToPayload(byte rawMessage[], byte payloadBuffer[], int startAt, int length) {

	    int insertAt = startAt;
        for ( int copy = 0; copy < length; copy++, insertAt++ ) {
            payloadBuffer[insertAt] = rawMessage[copy];
        }
        return length;
    }

    public int deflateMessageToPayload(byte rawMessage[], byte payloadBuffer[], int startAt, int length) throws Exception {

        // And then compress the response and send it out.
        Deflater compressor = new Deflater(Deflater.DEFLATED, true);
        compressor.setInput(rawMessage);
        compressor.finish();
        int compLength = compressor.deflate(payloadBuffer, startAt, length);
        compressor.end();
        return compLength;
    }

    public int gzipMessageToPayload(byte rawMessage[], byte payloadBuffer[], int startAt, int length) throws Exception {

        // And then compress the response and send it out.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream gzipOut = new GZIPOutputStream(baos);
        gzipOut.write(rawMessage, 0, length);
        gzipOut.close();
        byte[] tempBuffer = baos.toByteArray();
        int insertAt = startAt;
        for ( int copy = 0; copy < tempBuffer.length; copy++, insertAt++ ) {
            payloadBuffer[insertAt] = tempBuffer[copy];
        }
        return tempBuffer.length;
    }

    public void maskMessagePayload(byte mask[], byte rawMessage[], int startAt, int length) throws Exception {

        for (int maskbyte = 0; maskbyte < length; maskbyte++) {

            rawMessage[startAt + maskbyte] = (byte) (rawMessage[startAt + maskbyte] ^ mask[(maskbyte % 4)]);
        }
    }

    protected String extractMessage(InputStream rawIn) {
        boolean bOpen = true;
        String message = "";

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
            return "";
        }

        if (lengthCode == 0) {
            payloadSize = 0;
            return "";
        } else if (lengthCode < 126) {
            payloadSize = lengthCode;
        } else {
            if (lengthCode == 126) {
                try {
                    chunk = rawIn.read(buffer, 2, 2);
                } catch (IOException ioEx) {
                    return "";
                }
                payloadSize = ((0xFF & buffer[2]) << 8) + (0xFF & buffer[3]); // Adding 2 bytes minimum frame start.
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

        return message;
    }
}