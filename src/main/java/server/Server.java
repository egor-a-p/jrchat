package server;

import connection.Connection;
import message.Message;
import message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;


public class Server {
    private static final Logger LOG = LoggerFactory.getLogger(Server.class);
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        int port = Integer.parseInt(loadServerProperties().getProperty("port"));

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            LOG.info("Server running on port: " + port);

            Thread botThread = new Thread(new DateBot());
            botThread.setDaemon(true);
            botThread.start();

            while (true) {
                Socket socket = serverSocket.accept();
                new Handler(socket).start();
            }
        } catch (IOException e) {
            LOG.error("Server error :: ", e);
        }

    }

    public static Properties loadServerProperties() {
        Properties properties = new Properties();
        try (InputStream in = Server.class.getClassLoader().getResourceAsStream("connection.properties")) {
            properties.load(in);
        } catch (IOException e) {
            LOG.error("Can't load server properties :: ", e);
        }

        return properties;
    }

    public static void sendBroadcastMessage(Message message) {
        try {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet()) {
                entry.getValue().send(message);
            }
        } catch (IOException e) {
            LOG.error("Can't send broadcast message :: ", e);
        }
    }

    private static class Handler extends Thread {
        private Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            String userName = null;
            try (Connection connection = new Connection(socket)) {
                LOG.info("New connection with: " + socket.getRemoteSocketAddress());
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                sendListOfUsers(connection, userName);
                serverMainLoop(connection, userName);
            } catch (Exception e) {
                LOG.info("Invalid remote exchange");
            }

            if (userName != null) {
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }

            LOG.info("Connection closed.");

        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true) {
                connection.send(new Message(MessageType.NAME_REQUEST));
                Message message = connection.receive();

                if (message.getType() == MessageType.USER_NAME &&
                        message.getData() != null &&
                        !message.getData().isEmpty() &&
                        !connectionMap.containsKey(message.getData())) {
                    connectionMap.put(message.getData(), connection);
                    connection.send(new Message(MessageType.NAME_ACCEPTED));
                    return message.getData();
                }
            }
        }

        private void sendListOfUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry<String, Connection> entry : connectionMap.entrySet())
                if (!entry.getKey().equals(userName))
                    connection.send(new Message(MessageType.USER_ADDED, entry.getKey()));
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT) {
                    Server.sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                } else {
                    LOG.info("Invalid message type.");
                }
            }
        }
    }
}
