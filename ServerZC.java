/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ZeroCopyDownload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.time.LocalDateTime;

/**
 *
 * @author Patchara
 */
public class ServerZC {

    public static void main(String[] args) {
        //load server config
        ServerConfig scf = new ServerConfig();
        //Set up - Admin can set file path and port at class ServerConfig
        int port = scf.getPort();
        File serverFolder = new File(scf.getServerFolder());
        //Time stamp
        LocalDateTime timeStamp = LocalDateTime.now();
        try {
            //Open Connection
            ServerSocketChannel serverSock = ServerSocketChannel.open();
            serverSock.bind(new InetSocketAddress(port));
            System.out.println(timeStamp + ": Server started.");
            //Wait for connection
            while (true) {
                SocketChannel clientSock = serverSock.accept();
                if (clientSock.isConnected()) {
                    System.out.println(timeStamp + ": Client accept. " + clientSock.getLocalAddress());
                }
                //New client thread
                ClientHandler handler = new ClientHandler(clientSock, serverFolder);
                handler.start();
            }
        } catch (IOException ex) {
            System.out.println(timeStamp + ": Cannot start the server.");
            //System.out.println(ex);
        }
    }
}

class ServerConfig {

    private final int port = 9000;
    private final String serverFolder = "//change to your dir";

    public String getServerFolder() {
        return serverFolder;
    }

    public int getPort() {
        return port;
    }
}

class ClientHandler extends Thread {

    File serverFolder;
    SocketChannel clientSock;

    public ClientHandler(SocketChannel clientSock, File serverFolder) {
        this.clientSock = clientSock;
        this.serverFolder = serverFolder;
    }

    @Override
    public void run() {
        File[] files = serverFolder.listFiles();
        ByteBuffer byteBuff;
        OUTER:
        while (true) {
            try {
                //Time stamp
                LocalDateTime timeStamp = LocalDateTime.now();
                //Recive command from client
                byteBuff = ByteBuffer.allocate(32);
                clientSock.read(byteBuff);
                byteBuff.flip();
                String reciveComm = new String(byteBuff.array(), "UTF-8").trim();
                switch (reciveComm) {
                    case "Files":
                        System.out.println(timeStamp + ": Client " + clientSock.getLocalAddress() + " request downloadable file list.");
                        //Send the number of file to client
                        int numberOfFile = files.length;
                        byteBuff = ByteBuffer.allocate(4);
                        byteBuff.putInt(numberOfFile);
                        byteBuff.flip();
                        clientSock.write(byteBuff);
                        //Send all the file name to client
                        String fileName = "";
                        for (int i = 0; i < numberOfFile; i++) {
                            fileName += files[i].getName() + " ";
                        }
                        byteBuff = ByteBuffer.wrap(fileName.getBytes("UTF-8"));
                        clientSock.write(byteBuff);
                        break;
                    case "Download":
                        //Recive selected file name from client
                        byteBuff = ByteBuffer.allocate(32);
                        clientSock.read(byteBuff);
                        byteBuff.flip();
                        String selectedFile = new String(byteBuff.array(), "UTF-8").trim();
                        //Check if there are files on the server or not
                        int fileSize = 0;
                        for (File file : files) {
                            if (file.getName().equals(selectedFile)) {
                                fileSize = (int) file.length();
                                break;
                            }
                        }
                        //Send file size to client
                        byteBuff = ByteBuffer.allocate(4);
                        byteBuff.putInt(fileSize);
                        byteBuff.flip();
                        clientSock.write(byteBuff);
                        if (fileSize > 0) {
                            System.out.println(timeStamp + ": Client " + clientSock.getLocalAddress() + " request file " + selectedFile + " / File size = " + fileSize + " byte");
                            try (FileChannel input = new FileInputStream(serverFolder.getPath() + "/" + selectedFile).getChannel()) {
                                byteBuff = ByteBuffer.allocate(1500);
                                //int progress = 0;
                                //int startIndex = 0;
                                long start = System.currentTimeMillis();
                                /*
                                while (progress != fileSize) {
                                    progress += input.transferTo(startIndex, 1500, clientSock);
                                    startIndex += 1500;
                                }
                                 */
                                while (input.read(byteBuff) != -1) {
                                    byteBuff.flip();
                                    clientSock.write(byteBuff);
                                    byteBuff.clear();
                                }
                                long end = System.currentTimeMillis();
                                System.out.println(timeStamp + ": Finish tranafer file to client " + clientSock.getLocalAddress() + ", transfer time = " + (end - start) + "ms");
                            }
                        } else {
                            System.out.println(timeStamp + ": Client " + clientSock.getLocalAddress() + " request file " + selectedFile + " / File not found.");
                        }
                        break;
                    case "Exit":
                        System.out.println(timeStamp + ": Client " + clientSock.getLocalAddress() + " disconnected. " + clientSock.getLocalAddress());
                        break OUTER;
                    default:
                        break;
                }
            } catch (IOException ex) {
                //System.out.println(ex);
            }
        }
    }
}
