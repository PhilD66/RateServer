package uk.co.eskabe.RateServer;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created by Phil on 02/05/2017.
 */
public class RateObject {
    private String instrument = "";
    private String near = "";
    private String far = "";
    private double price = 0.0;
    private ArrayList<RateUpdateListener> updateListeners = new ArrayList<RateUpdateListener>();

    public RateObject() {}

    public RateObject(String strInstrument, String strFxPair ) {
        setInfo(strInstrument, strFxPair);
    }

    public void setInfo( String strInsrtument, String strNearAndFar )  throws IndexOutOfBoundsException {
        if ( strNearAndFar.length() == 6 ) {
            instrument = strInsrtument;
            near = strNearAndFar.substring(0, 3);
            far = strNearAndFar.substring(3, 6);
        } else {
            throw new IndexOutOfBoundsException("Unexpected fxPair name length!");
        }
    }

    public String getInfo() { return instrument + "/" + near + far; }

    public void setPrice( double newPrice ) {
        price = newPrice;
        Iterator iListeners = updateListeners.iterator();
        while ( iListeners.hasNext() ) {
            RateUpdateListener thisListener = (RateUpdateListener)iListeners.next();
            if ( thisListener != null ) {
                thisListener.rateUpdated(this);
            }
        }
    }

    public String getPriceAsString() { return String.valueOf(price); }

    public boolean rateObjectEquals(String strCompare) {
        return ( getInfo().compareTo(strCompare) == 0 );
    }

    public void addListener( RateUpdateListener addListener ) {
        updateListeners.add( addListener );
    }

    public void removeListener( RateUpdateListener removeListener ) {
        updateListeners.remove(removeListener);
    }

    public String getInstrument() { return instrument; }

    public String getFxPair() { return near + far; }

    public double getPrice() { return price; }

    static public String makeRateObjectDefinition(String strInstrument, String strFxPair) { return strInstrument + "/" + strFxPair; }
}
