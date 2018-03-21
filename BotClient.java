package client;

import main.ConsoleHelper;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class BotClient extends Client{

    public static void main(String[] args) {
        BotClient botClient = new BotClient();
        botClient.run();
    }

    @Override
    protected SocketThread getSocketThread() {
        return new BotSocketThread();
    }

    @Override
    protected boolean shouldSendTextFromConsole() {
        return false;
    }

    @Override
    protected String getUserName(){
        return "date_bot_"+(int) (Math.random()*100);
    }

    public class BotSocketThread extends SocketThread{
        @Override
        protected void clientMainLoop() throws IOException, ClassNotFoundException {
            BotClient.this.sendTextMessage("Привет чатику. Я бот. Понимаю команды: дата, день, месяц, год, время, час, минуты, секунды.");
            super.clientMainLoop();
        }

        @Override
        protected void processIncomingMessage(String message) {
            ConsoleHelper.writeMessage(message);
            String[] array = message.split(": ");
            if (array.length == 2) {
                String name = array[0];
                String text = array[1];
                String dateFormat = null;
                switch (text) {
                    case "дата":
                        dateFormat = "d.MM.YYYY";
                        break;
                    case "день":
                        dateFormat = "d";
                        break;
                    case "месяц":
                        dateFormat = "MMMM";
                        break;
                    case "год":
                        dateFormat = "YYYY";
                        break;
                    case "время":
                        dateFormat = "H:mm:ss";
                        break;
                    case "час":
                        dateFormat = "H";
                        break;
                    case "минуты":
                        dateFormat = "m";
                        break;
                    case "секунды":
                        dateFormat = "s";
                        break;
                }
                if (dateFormat != null) {
                    String simpleDateFormat = new SimpleDateFormat(dateFormat).format(Calendar.getInstance().getTime());
                    String answer = "Информация для " + name + ": " + simpleDateFormat;
                    sendTextMessage(answer);
                }
            }
        }
    }
}
