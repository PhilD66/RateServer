package uk.co.eskabe.RateServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;

/**
 * Created by Phil on 15/04/2017.
 */
public class WebSocketMessageHandler {

    private InputStream inputStream = null;
    public int payloadLength = 0;
    public byte[] maskBytes = new byte[4];
    public boolean bIsText = false;
    public boolean bMask = false;
    public boolean bCompressionRequested = false;
    public boolean bCompressingOutbound = false;
    public boolean bCompressed = false;

    public WebSocketMessageHandler(InputStream useInputStream) {
        inputStream = useInputStream;
    }

    public int waitForMessage() throws WebsocketProtocolException, IOException {

        byte buffer[] = new byte[10];

        // Read only 2 bytes which is the minimum raw header
        int bytesRead = inputStream.read(buffer, 0, 2);

        if ( bytesRead != 2 ) {
            throw new WebsocketProtocolException("Unexpected byte count.");
        }

        int lengthCode = (0x7F & buffer[1]);
        byte opcode = (byte) (0x0F & buffer[0]);
        bCompressed = (0x40 & buffer[0]) == 0x40;
        bMask = ((0x80 & buffer[1]) != 0);

        switch( opcode ) {
            case 0x0:
                // Continuation frame. TBD!
                break;
            case 0x1:
                // Text message indicated
                bIsText = true;
                break;
            case 0x2:
                // Binary message type.
                break;
            case 0x8:
                return 8;
            case 0x9:
                // Ping. TBD!
                break;
            case 0xA:
                // Pong. TBD.
                break;
        }

        switch( lengthCode ) {
            case 0:
                throw new WebsocketProtocolException("Zero length payload detected.");
            case 127:
                throw new WebsocketProtocolException("Unhandled scenario! Over-large payload detected.");
            case 126:
                // Read two more bytes where the actual message length is stored.
                bytesRead = inputStream.read(buffer, 2, 2);
                if ( bytesRead != 2 ) {
                    throw new WebsocketProtocolException("Unintelligble header.");
                }
                payloadLength = ((0xFF & buffer[2]) << 8) + (0xFF & buffer[3]) + 2; // Adding 2 bytes minimum frame start.
                break;
            default:
                // Length > 0 but less than 126 bytes.
                payloadLength = lengthCode;
                break;
        }

        if (bMask) {
            bytesRead = inputStream.read(maskBytes, 0, 4);
        }

        return opcode;
    }

    public int getErrorCode() throws WebsocketProtocolException, IOException {
        byte errorBytes[] = new byte[2];
        int bytesRead = inputStream.read(errorBytes, 0, 2);
        if ( bytesRead != 2 ) {
            throw new WebsocketProtocolException("Unexpected byte count.");
        }
        return (int)(((0xFF & errorBytes[0]) << 8) + (0xFF & errorBytes[1]));
    }

    public String getTextMessage() throws WebsocketProtocolException, IOException {
        // Now we know the length of the payload we can allocate a buffer for it.
        byte payloadBuffer[] = new byte[payloadLength];

        // Read data - note: may not read fully (or evenly), read from stream until len==0
        int len, offset = 0;
        while ((len = inputStream.read(payloadBuffer, offset, payloadBuffer.length - offset)) > 0) {
            offset += len;
        }

        if (bMask) {
            int oct = 0;
            for (int mbyte = 0; mbyte < payloadLength; mbyte++) {
                payloadBuffer[mbyte] = (byte) (payloadBuffer[mbyte] ^ maskBytes[oct % 4]);
                oct++;
            }
        }

        byte[] uncompressedData = null;
        int resultLength = 0;
        // Only works with Chrome which seems to insist on compression.
        if (bCompressed) {
            Inflater decompresser = new Inflater(true);
            decompresser.setInput(payloadBuffer, 0, (int) payloadLength);
            uncompressedData = new byte[2 * payloadLength]; // Assume maximum of 2:1 compression.
            try {
                resultLength = decompresser.inflate(uncompressedData);
            } catch (DataFormatException dfEx) {
                throw new WebsocketProtocolException("ZIP data format exception.");
            }
            decompresser.finished();
        } else {
            resultLength = payloadLength;
            uncompressedData = new byte[payloadLength]; // Assume maximum of 2:1 compression.
            for (int copy = 0; copy < payloadLength; copy++) {
                uncompressedData[copy] = payloadBuffer[copy];
            }
        }

        /*
        offset = 0;
        if ( bOpen == false ) {
            // Close request being processed.
            int closeCode = ((0xFF & uncompressedData[0]) << 8) + (0xFF & uncompressedData[1]);
            offset = 2;
        }
        */
        String message = new String(uncompressedData, 0, resultLength, "UTF-8");
        return message;

    }

    public byte[] getBinaryMessage() {
        return new byte[2];
    }

    public void sendMessage(String txMessage, OutputStream streamOut) {
        if ( bCompressionRequested && bCompressingOutbound ) {
            sendMessageDeflated(txMessage, streamOut);
        } else {
            sendMessageUncompressed(txMessage, streamOut);
        }
    }

    protected void sendMessageUncompressed(String txMessage, OutputStream streamOut) {
        // Do something with the message here...

        System.out.println("Message back to client is: " + txMessage);

        // And then compress the response and send it out.
        Deflater compressor = new Deflater(Deflater.DEFLATED);
        try {
            int headerLength = 2;
            byte unzippedMsg[] = txMessage.getBytes("UTF-8");
            //compressor.setInput(unzippedMsg);
            //compressor.finish();
            byte zippedMsg[] = new byte[txMessage.length() * 2 + headerLength];
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

    protected void sendMessageDeflated(String txMessage, OutputStream streamOut) {

        System.out.println("Message back to client is: " + txMessage);

        // And then compress the response and send it out.
        Deflater compressor = new Deflater(Deflater.DEFLATED, true);
        try {
            int headerLength = 2;
            byte unzippedMsg[] = txMessage.getBytes("UTF-8");
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

    protected void sendMessageGZipped(String txMessage, OutputStream streamOut) {
        // Do something with the message here...

        System.out.println("Message back to client is: " + txMessage);

        // And then compress the response and send it out.
        try {
            int headerLength = 2;
            byte unzippedMsg[] = txMessage.getBytes("UTF-8");
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

}
