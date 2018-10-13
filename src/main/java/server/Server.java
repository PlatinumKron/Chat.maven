package server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import network.TCP;
import network.TCPObserver;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;

public class Server implements TCPObserver {

    private  static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void main(String[] args) {
        new Server();
    }

    private final ArrayList<TCP> connections = new ArrayList<>();

    private Server() {
        System.out.println("Server Running...");
        try (ServerSocket serverSocket = new ServerSocket(8228)) {
            while (true) {
                try {
                    new TCP(this, serverSocket.accept());
                } catch (IOException e) {
                    System.out.println("TCP Connection exception: " + e);
                }
            }
        } catch (IOException e) {
            throw  new RuntimeException(e);
        }
    }

    // Реализация H2 in-memory
    private String url = "jdbc:h2:mem:";
    private Statement st;
    private int id = 1;
    private Connection con;

    {
        try {
            con = DriverManager.getConnection(url);
            st = con.createStatement();
            String sql =  "CREATE TABLE   MESSAGES " +
                    "(id INTEGER not NULL, " +
                    " msg TEXT, " +
                    " PRIMARY KEY ( id ))";
            st.executeUpdate(sql);
        } catch (SQLException ex) {
        }
    }

    private FileReader file;

    {
        try {
            file = new FileReader("validNames.json");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
    private nameWrapper names = GSON.fromJson(file, nameWrapper.class);

    //Переопределение методов интерфейса
    @Override
    public synchronized void OnConnectionReady(TCP tcp) {
        connections.add(tcp);
        sendAll("Client connected: " + tcp);

            // Отправка истории сообщений из БД новому клиенту
            try {
                ResultSet rs = st.executeQuery("SELECT * FROM MESSAGES");
                for (;;) {
                    if (rs.next()) {

                        sendHistory(rs.getString("MSG"));
                    }
                    else {
                        rs.close();
                        break;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
    }

    @Override
    public synchronized void OnReceiveString(TCP tcp, String value) {
        for (String name:names.names) {
            if (value.split(":")[0].equals(name)) {
                sendAll(value);


                //Запись в  БД нового сообщения
                try {
                    int result = st.executeUpdate("INSERT INTO MESSAGES VALUES ("+id+",'"+value+"')");
                    id++;
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public synchronized void onDisconnect(TCP tcp) {
        connections.remove(tcp);
        sendAll("Client disconnected: " + tcp);
    }

    @Override
    public synchronized void onException(TCP tcp, Exception e) {
        System.out.println("TCP Connection exception:" + e);
    }

    private void sendAll(String value) {
        System.out.println(value);
        final  int cnt = connections.size();
        for (int i=0; i < cnt; i++) {
            connections.get(i).sendString(value);
        }
    }
    private void sendHistory(String value) {
        final int cnt = connections.size();
        connections.get(cnt - 1).sendString(value);
    }
}

class nameWrapper {

    public ArrayList<String> names;
}
