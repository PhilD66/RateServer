package uk.co.eskabe.RateServer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Created by Phil on 23/04/2017.
 */
public class JsonSerializerBaseTest extends TestCase
{
    protected class InnerThing extends JsonSerializerBase {
        public int usefulInt = 99;
        private int arrayOfInt[] = new int[5];
    }

    protected class Test1 extends JsonSerializerBase {
        public String tom = "Tom";
        protected String dick = "Dick";
        public String harry = "Harry";
        public long aValue = 10;
        public InnerThing inner;

        public Test1() {
            inner = new InnerThing();
        }
    }

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public JsonSerializerBaseTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( JsonSerializerBaseTest.class );
    }

    /**
     * Rigourous Test :-)
     */
    public void testSerializeWrite()
    {
        Test1 test1 = new Test1();
        String output1 = test1.writeOut().replace(" ", "").replace("\t", "").replace("\r", "").replace("\n", "");

        assertEquals( "{\"tom\":\"Tom\",\"dick\":\"Dick\",\"harry\":\"Harry\",\"aValue\":\"10\",\"inner\":{\"usefulInt\":\"99\",\"arrayOfInt\":[\"0\",\"0\",\"0\",\"0\",\"0\"]}}", output1 );
    }
}
