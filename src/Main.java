import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main extends Thread {
    public final String name;
    public static void main(String[] args) {
        Main mainThread = new Main(args[0]);
        mainThread.start();
    }

    public Main(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        System.out.println("Program started");
        this.createLocalServerSocket();
    }

    public void createWritingThread() {
        Writer writingThread = new Writer(this.name);
        writingThread.start();
    }

    public void createLocalServerSocket() {
        try {
            ServerSocket listeningSocket = new ServerSocket(0);
            System.out.println("Chat App started on port " + listeningSocket.getLocalPort());
            this.createWritingThread();
            Socket connectionSocket = listeningSocket.accept();
            ObjectOutputStream outputStream = new ObjectOutputStream(connectionSocket.getOutputStream());
            ObjectInputStream inputStream = new ObjectInputStream(connectionSocket.getInputStream());
            String companionName = (String) inputStream.readObject();
            while (true) {
                String receivedMessage = (String) inputStream.readObject();
                System.out.println(companionName + ": " + receivedMessage);
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public class Writer extends Thread {
        private static final String SERVER_IP_ADDRESS = "127.0.0.1";
        public final String name;
        public Writer(String name){
            this.name = name;
        }

        @Override
        public void run() {
            this.performCompanionConnectionAndWriting();
        }

        public void performCompanionConnectionAndWriting() {
            try {
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                System.out.println("Enter the port number of the companion socket you want to chat with: ");
                String enteredUserCommand = bufferedReader.readLine();
                int companionPortNumber = Integer.parseInt(enteredUserCommand);
                Socket connectionSocket = new Socket(SERVER_IP_ADDRESS, companionPortNumber);
                System.out.println("Connected!");
                ObjectOutputStream outputStream = new ObjectOutputStream(connectionSocket.getOutputStream());
                ObjectInputStream inputStream = new ObjectInputStream(connectionSocket.getInputStream());
                outputStream.flush();
                writeTo(outputStream, this.name);
                while (true) {
                    String message = bufferedReader.readLine();
                    writeTo(outputStream, message);
                }
            } catch (NumberFormatException exception) {
                exception.printStackTrace();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public void writeTo(ObjectOutputStream outputStream, String message){
        try{
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException exception){
            exception.printStackTrace();
        }
    }
}
