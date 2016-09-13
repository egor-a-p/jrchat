package server;

import client.ClientDraft;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateBot extends ClientDraft implements Runnable{

    @Override
    protected String getUserName() {
        return "date_bot";
    }

    @Override
    protected String getServerAddress() {
        return "localhost";
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    public void run() {
        try {
            Thread thread = getSocketThread();
            thread.setDaemon(true);
            thread.start();

            synchronized (this) {
                while (!clientConnected)
                    this.wait();
            }

            if (!clientConnected)
                LOG.error("Bot runtime error.");
            else {
                while (true) {
                }
            }

        } catch (InterruptedException e) {
            LOG.error("Connection error.");
        }
    }

    public class BotSocketThread extends SocketThread {

        @Override
        protected void processIncomingMessage(String message) {
            super.processIncomingMessage(message);

            if (!message.contains(": "))
                return;

            int index = message.indexOf(": ");
            String userName = message.substring(0, index);
            String request = message.substring(index + 2).trim();
            SimpleDateFormat dateFormat;

            switch (request) {
                case "дата":
                    dateFormat = new SimpleDateFormat("d.MM.YYYY");
                    break;
                case "день":
                    dateFormat = new SimpleDateFormat("d");
                    break;
                case "месяц":
                    dateFormat = new SimpleDateFormat("MMMM");
                    break;
                case "год":
                    dateFormat = new SimpleDateFormat("YYYY");
                    break;
                case "время":
                    dateFormat = new SimpleDateFormat("H:mm:ss");
                    break;
                case "час":
                    dateFormat = new SimpleDateFormat("H");
                    break;
                case "минуты":
                    dateFormat = new SimpleDateFormat("m");
                    break;
                case "секунды":
                    dateFormat = new SimpleDateFormat("s");
                    break;
                default:
                    return;
            }

            sendTextMessage(String.format("Информация для %s: %s", userName, dateFormat.format(Calendar.getInstance().getTime())));
        }
    }
}
