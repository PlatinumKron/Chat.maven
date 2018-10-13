package client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import network.TCP;
import network.TCPObserver;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;

public class ClientWindow extends JFrame implements ActionListener, TCPObserver {

    private  static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int WIDTH = 600;
    private static final int HEIGHT = 400;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new ClientWindow();
            }
        });
    }

    private FileReader file;

    {
        try {
            file = new FileReader("properties.json");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private Properties properties = GSON.fromJson(file, Properties.class);
    private final JTextArea log = new JTextArea();
    private final JTextField input = new JTextField("Enter Text:");
    private final JButton send = new JButton("Send");
    private JTextField name = new JTextField(properties.NAME);


    private TCP connection;

    private  ClientWindow() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(WIDTH,HEIGHT);
        setTitle("CHAT");
        setLocationRelativeTo(null);
        setAlwaysOnTop(true);
        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane jsp = new JScrollPane(log);
        add(jsp, BorderLayout.CENTER);
        JPanel bottomPanel = new JPanel(new BorderLayout());
        add(bottomPanel, BorderLayout.SOUTH);
        bottomPanel.add(send, BorderLayout.EAST);
        bottomPanel.add(input, BorderLayout.CENTER);
        input.addActionListener(this);
        bottomPanel.add(name, BorderLayout.WEST);
        setVisible(true);

        try {
            String IP = properties.IP;
            int PORT = properties.PORT;
            connection = new TCP(this, IP, PORT);
        } catch (IOException e) {
            printMsg("Connection exception" + e);
        }
    }
    // Отправка Enter
    @Override
    public void actionPerformed(ActionEvent e) {
        String msg = input.getText();
        if (msg.equals("")) return;
        input.setText(null);
        connection.sendString(name.getText() + ": " + msg);
        input.grabFocus();
        // Кнопка Отправить
        send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String msg = input.getText();
                if (msg.equals("")) return;
                input.setText(null);
                connection.sendString(name.getText() + ": " + msg);
                input.grabFocus();
            }
        });
    }

    @Override
    public void OnConnectionReady(TCP tcp) {
        printMsg("Connection ready...");
    }

    @Override
    public void OnReceiveString(TCP tcp, String value) {
        printMsg(value);
    }

    @Override
    public void onDisconnect(TCP tcp) {
        printMsg("Connection close");
    }

    @Override
    public void onException(TCP tcp, Exception e) {
        printMsg("Connection exception" + e);
    }

    private synchronized void printMsg(String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }
}
class Properties {

    String IP;
    Integer PORT;
    String NAME;
}
