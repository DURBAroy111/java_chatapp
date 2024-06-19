import javax.swing.*;

import java.net.*;
import java.awt.*;
import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ChatServer {
    private static final int PORT = 5000;

    private static Set<ClientHandler> clientHandlers = Collections.synchronizedSet(new HashSet<>());

    private static JTextArea statusArea;

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> createAndShowGUI());

        try {
            ServerSocket serverSocket = new ServerSocket(PORT);
            System.out.println("Server started on port " + PORT);
            updateStatus("Server started on port " + PORT);



            while (true) {

                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientHandlers);
                clientHandlers.add(clientHandler);
                clientHandler.start();
                updateStatus("One client connected.");
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    private static void createAndShowGUI() {

        JFrame frame = new JFrame("Chat Server");

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setLayout(new BorderLayout());

        statusArea = new JTextArea();
        statusArea.setEditable(false);
        statusArea.setFont(new Font("Serif", Font.PLAIN, 18));
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);

        JScrollPane scrollPane = new JScrollPane(statusArea);

        frame.add(scrollPane, BorderLayout.CENTER);

        JLabel portLabel = new JLabel("The server is running on port " + PORT, SwingConstants.CENTER);
        portLabel.setFont(new Font("Serif", Font.BOLD, 20));
        frame.add(portLabel, BorderLayout.NORTH);

        frame.setSize(600, 400);

        frame.setVisible(true);
    }


    private static void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            statusArea.append(message + "\n");

            statusArea.setCaretPosition(statusArea.getDocument().getLength());
        });
    }
}

