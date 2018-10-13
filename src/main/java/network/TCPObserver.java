package network;

public interface TCPObserver {

    void OnConnectionReady (TCP tcp);
    void OnReceiveString (TCP tcp, String value);
    void onDisconnect (TCP tcp);
    void onException (TCP tcp, Exception e);

}
