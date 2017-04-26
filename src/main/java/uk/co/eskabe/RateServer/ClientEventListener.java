package uk.co.eskabe.RateServer;

/**
 * Created by Phil on 30/03/2017.
 */
public interface ClientEventListener {
    public void onMessage(String rxMessage);
    public void onConnectionClosed(ClientEventListener listener);
}

