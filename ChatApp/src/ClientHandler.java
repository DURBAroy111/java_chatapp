import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

public class ClientHandler extends Thread {
    private Socket socket;
    private Connection connection;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private Set<ClientHandler> clientHandlers;


    public ClientHandler(Socket socket, Set<ClientHandler> clientHandlers) {
        this.socket = socket;
        this.clientHandlers = clientHandlers;
        this.connection = DatabaseConnection.getInstance().getConnection();
    }


    public void run() {
        try {
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            out.println("Welcome to the Chat Server. Please register or login.");

            while (true) {
                String input = in.readLine();
                if (input.startsWith("register")) {
                    handleRegister(input);
                } else if (input.startsWith("login")) {
                    if (handleLogin(input)) {
                        break;
                    }
                }
            }
            notifyUserStatus(true);


            out.println("Login successful. You can now send messages and files.");
            updateUserList();

            String clientInput;
            while ((clientInput = in.readLine()) != null) {
                if (clientInput.startsWith("msg")) {
                    handleMessage(clientInput);
//                } else if (clientInput.startsWith("file")) {
//                    handleFile(clientInput);
                } else if (clientInput.startsWith("getChatHistory")) {
                    handleGetChatHistory(clientInput);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }


    private void handleRegister(String input) {
        try {
            String[] parts = input.split(" ");
            String username = parts[1];
            String password = parts[2];

            PreparedStatement stmt = connection.prepareStatement("INSERT INTO users (username, password) VALUES (?, ?)");
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.executeUpdate();
            out.println("Registration successful. You can now login.");
        } catch (SQLException e) {
            out.println("Registration failed: " + e.getMessage());
        }
    }



    private boolean handleLogin(String input) {
        try {
            String[] parts = input.split(" ");
            String username = parts[1];
            String password = parts[2];

            PreparedStatement stmt = connection.prepareStatement("SELECT * FROM users WHERE username = ? AND password = ?");
            stmt.setString(1, username);
            stmt.setString(2, password);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                this.username = username;
                out.println("Login successful. You can now send messages and files.");
                return true;
            } else {
                out.println("Login failed. Invalid username or password, or user not registered.");
                return false;
            }
        } catch (SQLException e) {
            out.println("Login failed: " + e.getMessage());

            return false;
        }
    }



    private void updateUserList() {
        try {
            PreparedStatement stmt = connection.prepareStatement("SELECT username FROM users");
            ResultSet rs = stmt.executeQuery();

            StringBuilder userList = new StringBuilder("Registered users:");
            while (rs.next()) {
                userList.append(" ").append(rs.getString("username"));
            }

            synchronized (clientHandlers) {
                for (ClientHandler clientHandler : clientHandlers) {

                    clientHandler.out.println(userList.toString());
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();

        }
    }

    private void handleMessage(String input) {
        try {
            String[] parts = input.split(" ", 3);
            String receiver = parts[1];

            String message = parts[2];

            PreparedStatement stmt = connection.prepareStatement("INSERT INTO messages (sender, receiver, message) VALUES (?, ?, ?)");
            stmt.setString(1, this.username);
            stmt.setString(2, receiver);
            stmt.setString(3, message);
            stmt.executeUpdate();

            synchronized (clientHandlers) {
                boolean receiverFound = false;
                for (ClientHandler clientHandler : clientHandlers) {
                    if (clientHandler.username.equals(receiver)) {
                        clientHandler.out.println( this.username + ": " + message);

                        receiverFound = true;

                    }
                }
                if (!receiverFound) {
//                    out.println("Message sent. User " + receiver + " is not online.");
                }

            }
        } catch (SQLException e) {
            out.println("Failed to send message: " + e.getMessage());

        }
    }




    private void handleGetChatHistory(String input) {
        try {
            String[] parts = input.split(" ");
            String targetUser = parts[1];

            PreparedStatement stmt = connection.prepareStatement("SELECT sender, message FROM messages WHERE (sender = ? AND receiver = ?) OR (sender = ? AND receiver = ?)");
            stmt.setString(1, this.username);
            stmt.setString(2, targetUser);
            stmt.setString(3, targetUser);
            stmt.setString(4, this.username);
            ResultSet rs = stmt.executeQuery();

            StringBuilder chatHistory = new StringBuilder();
            while (rs.next()) {
                chatHistory.append(rs.getString("sender")).append(": ").append(rs.getString("message")).append("\n");
            }

            out.println("Chat history with " + targetUser + ":\n" + chatHistory.toString());
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void notifyUserStatus(boolean isOnline) {
        String status = isOnline ? "online" : "offline";
        synchronized (clientHandlers) {
            for (ClientHandler clientHandler : clientHandlers) {
                if (!clientHandler.username.equals(this.username)) {
                    clientHandler.out.println(status + " " + this.username);
                }
            }
        }
    }



    private void cleanup() {
        try {
            if (username != null) {
                notifyUserStatus(false);
                clientHandlers.remove(this);
                updateUserList();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
