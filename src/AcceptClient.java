/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Meluleki
 */
public class AcceptClient extends Thread{
    
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
                return;
            }

        }
    }

    private void sendMessage(Message msg, int ortIndex) {
        try {
            CServer.outputstreams.get(ortIndex).reset();
            CServer.outputstreams.get(ortIndex).writeUnshared(msg);
            CServer.outputstreams.get(ortIndex).flush();
        } catch (IOException ex) {
            Logger.getLogger(AcceptClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    public void updateLists(Message msg) {
        int i=0;
        for(;i<CServer.loginNames.size();i++) {
            sendMessage(msg, i);
        }
    }

    public void recMessage(Message msg) {
        switch (msg.mType) {
            case Values.CONNECTIN_PROTOCOL: {
                String userTemp = msg.message;
                CServer.addClient(userTemp, obout);
                Message a = new Message(Values.OBJECTTYPE_LIST_PROTOCOL, msg.sender, Values.SERVER_USER_NAME, CServer.loginNames);
                updateLists(a);
                break;
            }
            case Values.TEXT_PROTOCOL: {
                String rec = msg.recipent;
                int sckNumber;
                for (String s : (ArrayList<String>) CServer.loginNames) {
                    if (s.equals(rec)) {
                        sckNumber = ((ArrayList<String>) CServer.loginNames).indexOf(s);
                        sendMessage(msg, sckNumber);
                        break;
                    }
                }
                break;
            }
            case Values.DISCONNECT_PROTOCOL: {
                try {
                    obin.close();
                    obout.close();
                    clientSocket.close();
                    CServer.removeClient(msg.sender);
                    this.cont = false;

                } catch (IOException ex) {
                    Logger.getLogger(AcceptClient.class
                            .getName()).log(Level.SEVERE, null, ex);
                }
                break;
            }

        }
    }

}
