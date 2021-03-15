/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ZeroCopyDownload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;
import java.text.DecimalFormat;
import java.util.Scanner;

/**
 *
 * @author Patchara
 */
public class ClientZC {

    public static void main(String[] args) {
        //load server config
        ClientConfig ccf = new ClientConfig();
        //Set up - Client can set place to store files, server and port at class ClientConfig
        int port = ccf.getPort();
        String server = ccf.getServer();
        File clientFolder = new File(ccf.getClientFolder());
        Scanner scan = new Scanner(System.in);
        OUTER:
        try {
            //Try to connect to server and Wait for user interaction
            SocketChannel clientSock = SocketChannel.open();
            clientSock.connect(new InetSocketAddress(server, port));
            if (clientSock.isConnected()) {
                System.out.println("Connected to server.");
            }
            ByteBuffer byteBuff;
            while (true) {
                System.out.print("Type Files to get file list.\n"
                        + "     Download to download file.\n"
                        + "     Exit to disconnect.\n"
                        + "Input = ");
                //Send command to server
                String sendComm = scan.nextLine();
                byteBuff = ByteBuffer.wrap(sendComm.getBytes("UTF-8"));
                clientSock.write(byteBuff);
                switch (sendComm) {
                    case "Files":
                        System.out.println("--- FILE ---");
                        //Recive the number of file from server
                        byteBuff = ByteBuffer.allocate(4);
                        clientSock.read(byteBuff);
                        byteBuff.flip();
                        int numberOfFile = byteBuff.getInt();
                        //Recvie all the file name from server
                        byteBuff = ByteBuffer.allocate(1024);
                        clientSock.read(byteBuff);
                        byteBuff.flip();
                        String[] fileName = new String(byteBuff.array(), "UTF-8").trim().split(" ");
                        for (int i = 0; i < numberOfFile; i++) {
                            System.out.println((i + 1) + ": " + fileName[i]);
                        }
                        break;
                    case "Download":
                        System.out.print("File name = ");
                        //Send selected file name to server
                        String selectedFile = scan.nextLine();
                        System.out.print("Name to save = ");
                        String nameToSave = scan.nextLine();
                        byteBuff = ByteBuffer.wrap(selectedFile.getBytes("UTF-8"));
                        clientSock.write(byteBuff);
                        //Recive file size from server
                        byteBuff = ByteBuffer.allocate(4);
                        clientSock.read(byteBuff);
                        byteBuff.flip();
                        int fileSize = byteBuff.getInt();
                        if (fileSize > 0) {
                            System.out.println("Downloading....");
                            try (FileChannel output = new FileOutputStream(clientFolder.getPath() + "/" + nameToSave, true).getChannel()) {
                                DecimalFormat df = new DecimalFormat("#.##");
                                byteBuff = ByteBuffer.allocate(1500);
                                int progress = 0;
                                int count = 0;
                                while (progress != fileSize) {
                                    progress += (int) clientSock.read(byteBuff);
                                    byteBuff.flip();
                                    output.write(byteBuff);
                                    byteBuff.clear();
                                    count++;
                                    if (count % 50 == 0) {
                                        System.out.println("Progress = " + df.format((progress / (double) fileSize) * 100) + "%");
                                    }
                                }
                            }
                            System.out.println("Finish.");
                        } else {
                            System.out.println("File not found.");
                        }
                        break;
                    case "Exit":
                        System.out.println("Disconnected from server.");
                        clientSock.close();
                        break OUTER;
                    default:
                        System.out.println("Wrong input.");
                        break;
                }
                System.out.println("");
            }
        } catch (IOException ex) {
            System.out.println("Can't connect to the Server.");
            //System.out.println(ex);
        }
    }
}

class ClientConfig {

    private final int port = 9000;
    private final String server = "127.0.0.1";
    private final String clientFolder = "//change to your dir";

    public int getPort() {
        return port;
    }

    public String getServer() {
        return server;
    }

    public String getClientFolder() {
        return clientFolder;
    }
}
