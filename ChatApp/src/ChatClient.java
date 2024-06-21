import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.HashSet;
import java.util.Set;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 5000;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;



    private JFrame frame;
    private CardLayout cardLayout;
    private JPanel mainPanel;
    private JTextField registerUsernameField;
    private JPasswordField registerPasswordField;
    private JTextField loginUsernameField;
    private JPasswordField loginPasswordField;
    private JTextArea chatArea;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JTextField messageField;
    private JButton sendButton;
    private JButton fileButton;



    private String username;
    private String currentChatUser;
    private File selectedFile;





    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatClient().createAndShowGUI());
    }

    private void createAndShowGUI() {
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        createRegisterPage();
        createLoginPage();
        createChatPage();

        frame.add(mainPanel);
        frame.setVisible(true);

        connectToServer();
    }

    private void createRegisterPage() {
        JPanel registerPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        registerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        registerPanel.add(new JLabel("Username:"));
        registerUsernameField = new JTextField();
        registerPanel.add(registerUsernameField);

        registerPanel.add(new JLabel("Password:"));
        registerPasswordField = new JPasswordField();
        registerPanel.add(registerPasswordField);

        JButton registerButton = new JButton("Register");
        registerButton.addActionListener(new RegisterActionListener());
        registerPanel.add(registerButton);

        JButton goToLoginButton = new JButton("Go to Login");
        goToLoginButton.addActionListener(e -> cardLayout.show(mainPanel, "login"));
        registerPanel.add(goToLoginButton);

        mainPanel.add(registerPanel, "register");
        cardLayout.show(mainPanel, "register");
    }

    private void createLoginPage() {
        JPanel loginPanel = new JPanel(new GridLayout(4, 2, 10, 10));
        loginPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        loginPanel.add(new JLabel("Username:"));
        loginUsernameField = new JTextField();
        loginPanel.add(loginUsernameField);

        loginPanel.add(new JLabel("Password:"));
        loginPasswordField = new JPasswordField();
        loginPanel.add(loginPasswordField);


        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(new LoginActionListener());
        loginPanel.add(loginButton);


        JButton goToRegisterButton = new JButton("Go to Register");
        goToRegisterButton.addActionListener(e -> cardLayout.show(mainPanel, "register"));
        loginPanel.add(goToRegisterButton);

        mainPanel.add(loginPanel, "login");

    }

    private void createChatPage() {
        JPanel chatPanel = new JPanel(new BorderLayout());

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);


        userList.setFont(new Font("Serif", Font.PLAIN, 18));

        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        chatPanel.add(new JScrollPane(userList), BorderLayout.WEST);

        JPanel chatAreaPanel = new JPanel(new BorderLayout());
        chatArea = new JTextArea();


        chatArea.setEditable(false);
        chatArea.setFont(new Font("Serif", Font.PLAIN, 18));

        chatAreaPanel.add(new JScrollPane(chatArea), BorderLayout.CENTER);

        JPanel messagePanel = new JPanel(new BorderLayout());
        messageField = new JTextField();

        messageField.setFont(new Font("Serif", Font.PLAIN, 18));


        messagePanel.add(messageField, BorderLayout.CENTER);

        sendButton = new JButton("Send");
        sendButton.setFont(new Font("Serif", Font.PLAIN, 18));
        sendButton.addActionListener(e -> sendMessage());
        messagePanel.add(sendButton, BorderLayout.EAST);


        fileButton = new JButton("+");
        fileButton.setFont(new Font("Serif", Font.PLAIN, 18));
        fileButton.addActionListener(e -> selectFile());

        messagePanel.add(fileButton, BorderLayout.WEST);

        chatAreaPanel.add(messagePanel, BorderLayout.SOUTH);
        chatPanel.add(chatAreaPanel, BorderLayout.CENTER);

        mainPanel.add(chatPanel, "chat");

        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 1) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null && !selectedUser.equals(currentChatUser)) {
                        currentChatUser = selectedUser;
                        getChatHistory(selectedUser);
                    }
                }
            }
        });
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new ServerListener().start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendMessage() {

        String message = messageField.getText();
        if (selectedFile != null) {

            sendFile();

        } else if (!message.isEmpty())
        {
            String receiver = currentChatUser;
            if (receiver != null)
            {
                chatArea.append("you: " + message + "\n");
                out.println("msg " + receiver + " " + message);
                messageField.setText("");

            }

            else
            {
                chatArea.append("Select a user to send a message.\n");
            }
        }
    }

    private void selectFile() {

        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            messageField.setText("Selected file: " + selectedFile.getName());

        }
    }

    private void sendFile() {
    }

    private void getChatHistory(String targetUser) {
        out.println("getChatHistory " + targetUser);
    }

    private class RegisterActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            String username = registerUsernameField.getText();
            String password = new String(registerPasswordField.getPassword());
            out.println("register " + username + " " + password);
            try {
                String response = in.readLine();
                chatArea.append(response + "\n");
                if (response.startsWith("Registration successful")) {
                    cardLayout.show(mainPanel, "login");
                }
            } catch (IOException ex) {
                ex.printStackTrace();

            }
        }
    }

    private class LoginActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            String username = loginUsernameField.getText();
            String password = new String(loginPasswordField.getPassword());
            out.println("login " + username + " " + password);
            try {
                String response = in.readLine();
                chatArea.append(response + "\n");
                if (response.startsWith("Login successful")) {
                    ChatClient.this.username = username;
                    cardLayout.show(mainPanel, "chat");
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    public class ServerListener extends Thread {
        public void run() {
            try {
                String input;
                while ((input = in.readLine()) != null) {
                    if (input.startsWith("Registered users:")) {
                        String[] users = input.substring(18).trim().split(" ");
                        userListModel.clear();
                        for (String user : users) {
                            if (!user.equals(username)) {
                                userListModel.addElement(user);
                            }
                        }
                        updateConnectedUsers();
                    }
                    else if (input.startsWith("Chat history with ")) {
                        chatArea.setText(""); // Clear chat area for new chat history
                        chatArea.append(input.substring(input.indexOf(":") + 1) + "\n");
                    } else {
                        chatArea.append(input + "\n");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void updateConnectedUsers() {
            SwingUtilities.invokeLater(() -> {
                userList.repaint();
            });
        }
    }
}


