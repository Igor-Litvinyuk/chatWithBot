package client;

import main.*;

import java.io.IOException;
import java.net.Socket;

public class Client {
    protected Connection connection;
    private volatile boolean clientConnected = false;

    public static void main(String[] args) {
        Client client = new Client();
        client.run();
    }

    public void run(){
        SocketThread socketThread = getSocketThread();
        socketThread.setDaemon(true);
        socketThread.start();

        synchronized (this){
            try {
                this.wait();
            } catch (InterruptedException e) {
                ConsoleHelper.writeMessage("Произошла ошибка!");
            }
        }

        if (clientConnected) {
            ConsoleHelper.writeMessage("Соединение установлено. Для выхода наберите команду 'exit'.");
        }
        else {
            ConsoleHelper.writeMessage("Произошла ошибка во время работы клиента.");
        }

        while (clientConnected){
            String lineText = ConsoleHelper.readString();
            if (lineText.equals("exit")){
                break;
            }
            if (shouldSendTextFromConsole()){
                sendTextMessage(lineText);
            }
        }
    }

    protected String getServerAddress(){
        ConsoleHelper.writeMessage("Введите адрес сервера:");
        return ConsoleHelper.readString();
    }

    protected int getServerPort(){
        ConsoleHelper.writeMessage("Введите порт сервера:");
        return ConsoleHelper.readInt();
    }

    protected String getUserName(){
        ConsoleHelper.writeMessage("Введите имя пользователя:");
        return ConsoleHelper.readString();
    }

    protected boolean shouldSendTextFromConsole(){
        return true;
    }

    protected SocketThread getSocketThread(){
        return new SocketThread();
    }

    protected void sendTextMessage(String text){
        try {
            connection.send(new Message(MessageType.TEXT, text));
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Ошибка при отправке сообщения!");
            clientConnected = false;
        }
    }

    public class SocketThread extends Thread {
        protected void processIncomingMessage(String message){
            ConsoleHelper.writeMessage(message);
        }

        protected void informAboutAddingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " присоединился к чату");
        }

        protected void informAboutDeletingNewUser(String userName){
            ConsoleHelper.writeMessage(userName + " покинул чат");
        }

        protected void notifyConnectionStatusChanged(boolean clientConnected){
            Client.this.clientConnected = clientConnected;
            synchronized (Client.this){
                Client.this.notify();
            }
        }

        protected void clientHandshake() throws IOException, ClassNotFoundException {
            Message message;
            while (!clientConnected){
                message = connection.receive();
                if (message.getType() == MessageType.NAME_REQUEST){
                    connection.send(new Message(MessageType.USER_NAME, getUserName()));
                }
                else if (message.getType() == MessageType.NAME_ACCEPTED){
                    notifyConnectionStatusChanged(true);
                }
                else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            while (!Thread.currentThread().isInterrupted()){
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT){
                    processIncomingMessage(message.getData());
                }
                else if (message.getType() == MessageType.USER_ADDED){
                    informAboutAddingNewUser(message.getData());
                }
                else if (message.getType() == MessageType.USER_REMOVED){
                    informAboutDeletingNewUser(message.getData());
                }
                else {
                    throw new IOException("Unexpected MessageType");
                }
            }
        }

        public void run() {
            try {
                connection = new Connection(new Socket(getServerAddress(),getServerPort()));
                clientHandshake();
                clientMainLoop();
            } catch (IOException e) {
                notifyConnectionStatusChanged(false);
                ConsoleHelper.writeMessage("Ошибка соединения!");
            } catch (ClassNotFoundException e) {
                notifyConnectionStatusChanged(false);
                ConsoleHelper.writeMessage("Ошибка подключения клиента к серверу");
            }

        }
    }
}
