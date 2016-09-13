package client;

import connection.Connection;
import message.Message;
import message.MessageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import server.Server;

import java.io.IOException;
import java.net.Socket;

public abstract class ClientDraft {
    private Connection connection;
    protected volatile boolean clientConnected = false;
    protected static final Logger LOG = LoggerFactory.getLogger(ClientDraft.class);

    abstract protected String getServerAddress();

    private int getServerPort(){
        return Integer.parseInt(Server.loadServerProperties().getProperty("port"));
    }

    abstract protected String getUserName();

    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    protected void sendTextMessage(String text){
        try {
            connection.send(new Message(MessageType.TEXT, text));
        }catch (IOException e){
            LOG.error("Can't send message.");
            clientConnected = false;
        }
    }

    abstract public void run();

    public class SocketThread extends Thread{

        @Override
        public void run() {
            try(Socket socket = new Socket(getServerAddress(), getServerPort())) {
                connection = new Connection(socket);
                clientHandshake();
                clientMainLoop();
            } catch (IOException | ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException{
            while (true) {
                Message message = connection.receive();

                switch (message.getType()) {
                    case NAME_REQUEST:
                        connection.send(new Message(MessageType.USER_NAME, getUserName()));
                        break;
                    case NAME_ACCEPTED:
                        notifyConnectionStatusChanged(true);
                        return;
                    default:
                        throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException{
            while (!Thread.currentThread().isInterrupted()) {
                Message message = connection.receive();

                switch (message.getType()) {
                    case TEXT:
                        processIncomingMessage(message.getData());
                        break;
                    case USER_ADDED:
                        informAboutAddingNewUser(message.getData());
                        break;
                    case USER_REMOVED:
                        informAboutDeletingNewUser(message.getData());
                        break;
                    default:
                        throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void processIncomingMessage(String message){
            LOG.info(message);
        }

        protected void informAboutAddingNewUser(String userName){
            LOG.info(userName +" connect.");
        }

        protected void informAboutDeletingNewUser(String userName){
            LOG.info(userName +" disconnect.");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            ClientDraft.this.clientConnected = clientConnected;
            synchronized (ClientDraft.this){
                ClientDraft.this.notify();
            }
        }

    }

}
