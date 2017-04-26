package uk.co.eskabe.RateServer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.json.simple.parser.ParseException;

/**
 * Created by Phil on 19/04/2017.
 */
public class MessageTranslatorTest extends TestCase {
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public MessageTranslatorTest( String testName ) {
        super( testName );
    }

        /**
         * @return the suite of tests being tested
         */
    public static Test suite()
    {
        return new TestSuite( MessageTranslatorTest.class );
    }

    /**
     * Rigorous Test :-)
     */
    public void testConnect() throws ParseException {
        MessageTranslator translator = new MessageTranslator();

        translator.parseInboundMessage("{ \"verb\":\"connect\", \"params\": {\"username\": \"sauron\", \"password\": \"of-mordor\"} }");

        assertTrue(translator.isConnect());
    }

    public void testConnectUsername() throws ParseException {
        MessageTranslator translator = new MessageTranslator();

        translator.parseInboundMessage("{ \"verb\":\"connect\", \"params\": {\"username\": \"sauron\", \"password\": \"of-mordor\"} }");

        assertEquals( translator.getUsername(),"sauron");
    }

    public void testFxRateSubscription() throws ParseException {
        MessageTranslator translator = new MessageTranslator();

        translator.parseInboundMessage("{ \"verb\":\"subscribe\", \"params\": {\"sessionId\": \"098765-1234-3456-4567890\", \"instrument\": \"spot\", \"fxPair\": \"GBPUSD\"} }");

        boolean bConnect = translator.isSubscribe();
        String sessionId = translator.getSessionId();
        String instr = translator.getInstrument();
        String fxpair = translator.getFxPair();
        assertTrue( bConnect && (instr.compareToIgnoreCase("spot") == 0) &&
                (fxpair.compareToIgnoreCase("GBPUSD") == 0) &&
                (sessionId.compareToIgnoreCase("098765-1234-3456-4567890") == 0));
    }

}
