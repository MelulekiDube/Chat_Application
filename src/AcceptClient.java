/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Meluleki
 */
public class AcceptClient extends Thread implements Cprotocol {

    public Socket clientSocket;
    public ObjectInputStream obin;
    public ObjectOutputStream obout;
    private boolean cont;

    public AcceptClient(Socket cs) throws IOException {
        this.cont = true;
        this.clientSocket = cs;
        this.clientSocket.setKeepAlive(true);
        this.obout = new ObjectOutputStream(cs.getOutputStream());
        obout.flush();
        this.obin = new ObjectInputStream(cs.getInputStream());
        System.out.println("here");

        start();

    }

    @Override
    public void run() {
        while (this.cont) {
            Message msgFromClien;
            try {
                msgFromClien = (Message) obin.readObject();
                recMessage(msgFromClien);
            } catch (IOException ex) {
                Logger.getLogger(AcceptClient.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                System.out.println("Client left successfully");
                return;
            }

        }
    }

    private void sendMessge(Message msg, Socket clientTO) throws IOException {
        ObjectOutputStream tempStream = new ObjectOutputStream(clientTO.getOutputStream());
        tempStream.writeObject(msg);
    }

    @Override
    public void sendMessage(Message msg) {
        try {
            obout.writeObject(msg);
            obout.flush();
        } catch (IOException ex) {
            Logger.getLogger(AcceptClient.class.getName()).log(Level.SEVERE, null, ex);
        }
        System.out.println("Done man");
    }

    @Override
    public void recMessage(Message msg) {
        System.out.println("Received");
        switch (msg.mType) {
            case Values.CONNECTIN_PROTOCOL: {
                System.out.println();
                String userTemp = msg.message;
                CServer.addClient(userTemp, clientSocket);
                sendMessage(new Message("Connected", userTemp));
                System.out.println(msg.message);
                ArrayList<String> r = new ArrayList<>();
                for (int i = 0; i < 5; i++) {
                    r.add("Mellar");
                }
                Message a = new Message(Values.OBJECTTYPE_LIST_PROTOCOL, msg.sender, Values.SERVER_USER_NAME, CServer.getUserList());
                sendMessage(a);
                break;
            }
            case Values.TEXT_PROTOCOL: {
                System.out.println(msg.message);
                String rec = msg.recipent;
                int sckNumber;
                for (String s : (ArrayList<String>) CServer.getUserList()) {
                    if (s.equals(rec)) {
                        sckNumber = ((ArrayList<String>) CServer.getUserList()).indexOf(s);
                        try {
                            sendMessge(msg, CServer.getClient(sckNumber));
                        } catch (IOException ex) {
                            Logger.getLogger(AcceptClient.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                break;
            }
            case Values.DISCONNECT_PROTOCOL: {
                try {
                    obin.close();
                    obout.close();
                    clientSocket.close();
                    this.cont = false;
                } catch (IOException ex) {
                    Logger.getLogger(AcceptClient.class.getName()).log(Level.SEVERE, null, ex);
                }
            }

        }
    }

}
