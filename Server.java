package main;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Server {
    private static Map<String, Connection> connectionMap = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        ConsoleHelper.writeMessage("Введите порт сервера:");
        int port = ConsoleHelper.readInt();
        try (ServerSocket serverSocket = new ServerSocket(port)){
            ConsoleHelper.writeMessage("Сервер запущен!");
            while (true){
                Socket socket = serverSocket.accept();
                Handler handler = new Handler(socket);
                handler.start();
            }
        } catch (IOException e) {
            ConsoleHelper.writeMessage("Произошла ошибка!");
        }
    }

    public static void sendBroadcastMessage(Message message){
        for (Map.Entry<String, Connection> connectionEntry : connectionMap.entrySet()){
            Connection connection = connectionEntry.getValue();
            try {
                connection.send(message);
            } catch (IOException e) {
                ConsoleHelper.writeMessage("Сообщение не отправлено!");
            }
        }
    }

    //inner static class:
    private static class Handler extends Thread {
        Socket socket;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            ConsoleHelper.writeMessage("Установлено соединение с удаленным клиентом с адресом: " + socket.getRemoteSocketAddress());
            Connection connection = null;
            String userName = null;
            try {
                connection = new Connection(socket);
                userName = serverHandshake(connection);
                sendBroadcastMessage(new Message(MessageType.USER_ADDED, userName));
                sendListOfUsers(connection, userName);
                serverMainLoop(connection, userName);
            } catch (IOException e) {

            } catch (ClassNotFoundException e) {

            }
            if (userName != null) {
                connectionMap.remove(userName);
                sendBroadcastMessage(new Message(MessageType.USER_REMOVED, userName));
            }
            ConsoleHelper.writeMessage("Соединение с удаленным адресом " + socket.getRemoteSocketAddress() + " закрыто!");
        }

        private String serverHandshake(Connection connection) throws IOException, ClassNotFoundException {
            while (true){
            connection.send(new Message(MessageType.NAME_REQUEST));
            Message message = connection.receive();
            String clientName = message.getData();
                if (message.getType() == MessageType.USER_NAME && clientName != null && !clientName.isEmpty() && connectionMap.get(clientName) == null){
                    connectionMap.put(clientName, connection);
                    connection.send(new Message(MessageType.NAME_ACCEPTED));
                    return clientName;
                }
            }
        }

        private void sendListOfUsers(Connection connection, String userName) throws IOException {
            for (Map.Entry<String, Connection> connectionEntry : connectionMap.entrySet()) {
                if (!userName.equals(connectionEntry.getKey())) {
                    connection.send(new Message(MessageType.USER_ADDED, connectionEntry.getKey()));
                }
            }
        }

        private void serverMainLoop(Connection connection, String userName) throws IOException, ClassNotFoundException {
            while (true) {
                Message message = connection.receive();
                if (message.getType() == MessageType.TEXT){
                    sendBroadcastMessage(new Message(MessageType.TEXT, userName + ": " + message.getData()));
                }
                else{
                    ConsoleHelper.writeMessage("Ошибка сообщения!");
                }
            }
        }
    }
}
