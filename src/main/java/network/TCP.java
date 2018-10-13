package network;
import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;

public class TCP {

    private final Socket socket;
    private final Thread rxThread;
    private final BufferedReader in;
    private final BufferedWriter out;
    private final TCPObserver eventObserver;

    // Конструктор создающий сокет
    public TCP(TCPObserver eventObserver, String ip, int port) throws IOException {
        this(eventObserver, new Socket(ip , port));
    }

    // Конструктор принимающий сокет
    public  TCP(TCPObserver eventObserver , Socket socket) throws IOException {
        this.eventObserver = eventObserver;
        this.socket = socket;
        in = new BufferedReader(new InputStreamReader(socket.getInputStream() , Charset.forName("UTF-8")));
        out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream() , Charset.forName("UTF-8")));
        rxThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    eventObserver.OnConnectionReady(TCP.this);
                    while (!rxThread.isInterrupted()) {
                        eventObserver.OnReceiveString(TCP.this, in.readLine());
                    }
                } catch (IOException e) {
                    eventObserver.onException(TCP.this, e);
                } finally {
                    eventObserver.onDisconnect(TCP.this);
                }
            }
        });
        rxThread.start();
    }

    public synchronized void  sendString(String value) {
        try {
            out.write(value + "\r\n");
            out.flush();
        } catch (IOException e) {
            eventObserver.onException(TCP.this, e);
            disconnect();
        }
    }

    public synchronized void disconnect() {
        rxThread.interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            eventObserver.onException(TCP.this, e);
        }
    }

    @Override
    public String toString() {
        return "TCP Connection: " + socket.getInetAddress() + ": " + socket.getPort();
    }
}
