import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main extends Thread {
    public final String name;
    private static final String SPACE_SEPARATOR = "\\s+";

    private static final String TRANSFER_COMMAND = "transfer";

    private static final int FILE_CHUNK_SIZE = 1024;

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
                String[] commandArguments = this.splitCommandBySeparator(receivedMessage, SPACE_SEPARATOR);
                if (commandArguments[0].equals(TRANSFER_COMMAND)) {
                    if (commandArguments.length != 2) {
                        System.err.println("Provided command does not follow the specified format of 2 arguments");
                        continue;
                    }
                    this.handleReceiveFile(commandArguments[1], inputStream);
                } else {
                    System.out.println(companionName + ": " + receivedMessage);
                }
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public void handleReceiveFile(String fileName, ObjectInputStream inputStream) {
        try {
            int byteCount = 0;
            String newFileName = "new" + Character.toUpperCase(fileName.charAt(0)) + fileName.substring(1, fileName.length());
            FileOutputStream fileOutputStream = new FileOutputStream(newFileName);
            long sizeOfFileLeft = inputStream.readLong();
            byte[] fileBuffer = new byte[FILE_CHUNK_SIZE];
            while (sizeOfFileLeft > 0 && (
                    byteCount = inputStream.read(fileBuffer, 0, (int) Math.min(FILE_CHUNK_SIZE, sizeOfFileLeft))
            ) != -1) {
                fileOutputStream.write(fileBuffer, 0, byteCount);
                sizeOfFileLeft -= byteCount;
            }
            System.out.println(fileName + " received successfully!");
            fileOutputStream.close();
        } catch (IOException ioException) {
            System.err.println("IO Exception encountered " + ioException.getMessage());
        }
    }

    public class Writer extends Thread {

        private static final String TRANSFER_COMMAND = "transfer";

        private static final String SPACE_SEPARATOR = "\\s+";
        private static final String SERVER_IP_ADDRESS = "127.0.0.1";
        private static final int FILE_CHUNK_SIZE = 1024;
        public final String name;

        public Writer(String name) {
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
                // To indicate my name to the chat companion
                writeTo(outputStream, this.name);
                while (true) {
                    String message = bufferedReader.readLine();
                    String[] commandArguments = splitCommandBySeparator(message, SPACE_SEPARATOR);
                    if (commandArguments[0].equals(TRANSFER_COMMAND)) {
                        if (commandArguments.length != 2) {
                            System.err.println("Provided command does not follow the specified format of 2 arguments");
                            continue;
                        }
                        this.sendFile(commandArguments, message, outputStream);
                    } else {
                        writeTo(outputStream, message);
                    }
                }
            } catch (NumberFormatException exception) {
                exception.printStackTrace();
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        public void sendFile(String[] commands, String message, ObjectOutputStream outputStream) {
            try {
                String fileName = commands[1];
                String projectRootFolderPath = (new File(".")).getCanonicalPath();
                File projectRootFolder = new File(projectRootFolderPath);
                File[] projectRootFolderFiles = projectRootFolder.listFiles();
                boolean wasFileFound = false;
                File requiredFile = null;
                for (File file : projectRootFolderFiles) {
                    if (file.getName().equals(fileName)) {
                        wasFileFound = true;
                        requiredFile = file;
                        break;
                    }
                }
                if (!wasFileFound) {
                    System.err.println("The requested file was not found");
                } else {
                    writeTo(outputStream, message);
                    int byteCount = 0;
                    FileInputStream fileInputStream = new FileInputStream(requiredFile);
                    long fileLength = requiredFile.length();
                    outputStream.writeLong(fileLength);
                    byte[] fileBuffer = new byte[FILE_CHUNK_SIZE];
                    while ((byteCount = fileInputStream.read(fileBuffer)) != -1) {
                        outputStream.write(fileBuffer, 0, byteCount);
                        outputStream.flush();
                    }
                    System.out.println("File sent successfully!");
                    fileInputStream.close();
                }
            } catch (IOException ioException) {
                System.err.println("IO Exception encountered " + ioException.getMessage());
            } catch (NullPointerException nullPointerException) {
                System.err.println("NullPointerException encountered " + nullPointerException.getMessage());
            }
        }
    }

    private String[] splitCommandBySeparator(String command, String separator) {
        return command.split(separator);
    }

    public void writeTo(ObjectOutputStream outputStream, String message) {
        try {
            outputStream.writeObject(message);
            outputStream.flush();
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
