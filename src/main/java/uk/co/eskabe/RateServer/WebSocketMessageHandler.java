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
    private OutputStream outputStream = null;
    public int payloadLength = 0;
    public byte[] maskBytes = new byte[4];
    public boolean bIsText = false;
    public boolean bMask = false;
    public boolean bCompressionRequested = false;
    static final int COMPRESSION_NONE = 0;
    static final int COMPRESSION_DEFLATE = 1;
    static final int COMPRESSION_GZIP = 2;
    protected int compression = COMPRESSION_NONE;
    public boolean bInboundIsCompressed = false;

    public WebSocketMessageHandler(InputStream useInputStream, OutputStream useOutputStream) {
        inputStream = useInputStream;
        outputStream = useOutputStream;
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
        bInboundIsCompressed = (0x40 & buffer[0]) == 0x40;
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
                payloadLength = ((0xFF & buffer[2]) << 8) + (0xFF & buffer[3]);
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
        if (bInboundIsCompressed) {
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

    public void sendMessage(String rxMessage) {
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

            outputStream.write(payload, maxHeaderLength - headerLength, actualPayloadLength + headerLength);

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
}
