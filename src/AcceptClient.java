/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import java.io.*;
import java.util.*;
/**
 *
 * @author Meluleki
 */
public class AcceptClient extends Thread {

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
        CServer.messageBuffer = new ArrayList<>();
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
                JOptionPane.showMessageDialog(null, ex.getMessage() + "\n" + ex.getCause());
                return;
            } catch (ClassNotFoundException ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage() + "\n" + ex.getCause());
                return;
            }

        }
    }

    private void sendMessage(Message msg, int ortIndex) {
        try {
//            System.out.println(msg);
            CServer.outputstreams.get(ortIndex).reset();
            CServer.outputstreams.get(ortIndex).writeUnshared(msg);
            CServer.outputstreams.get(ortIndex).flush();
        } catch (IOException ex) {
            System.out.println("Error");
        }
    }

    public void sendMessage(Message msg, ArrayList<String> listofuser) {
        for (String list : listofuser) {
            for (String loggedin : CServer.loginNames) {
                if (list.equals(loggedin)) {
                    Message m = new Message(Values.TEXT_PROTOCOL, loggedin, msg.sender, msg.message);
                    int i = CServer.loginNames.indexOf(loggedin);
                    sendMessage(m, i);
                }
            }
        }
    }

    public void sendFile(Message msg, ArrayList<String> list) {
        for (String lUsers : list) {
            for (String loggedin : CServer.loginNames) {
                if (list.equals(loggedin)) {
                    Message newMsge = new Message(Values.REQUEST_FILE_PROTOCOL, msg.recipent, Values.SERVER_USER_NAME, msg.message);
                    newMsge.fileNumber = (Object) CServer.messageBuffer.size();
                    CServer.messageBuffer.add(msg);
                    CServer.REQUEST_PENDING++;
                    sendMessage(newMsge, CServer.loginNames.indexOf(lUsers));
                    System.out.println("Requestion sent to" + lUsers);
                    break;
                }
            }
        }
    }

    public void updateLists(Message msg) {
        int i = 0;
        for (; i < CServer.loginNames.size(); i++) {
            sendMessage(msg, i);
        }
    }

    public void recMessage(Message msg) {
        switch (msg.mType) {
            case Values.CONNECTIN_PROTOCOL: {

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
            case Values.BRODACAST_TEXT_PROTOCOL: {
                sendMessage(msg, (ArrayList<String>) msg.obMessage);
                break;
            }
            case Values.BRODCAST_FILE_PROTOCOL: {
                String from = msg.sender;
                ArrayList<String> s = (ArrayList<String>) msg.obMessage;
                Message newMessage;
                for (String user : s) {
                    newMessage = new Message(Values.FILE_PROTOCOL, user, from, msg.message);
                    newMessage.file = msg.file;
                    recMessage(newMessage);
                }
                break;
            }
            case Values.FILE_PROTOCOL: {
                Message newMsge = new Message(Values.REQUEST_FILE_PROTOCOL, (String) msg.recipent, Values.SERVER_USER_NAME, msg.message);
                newMsge.fileNumber = (Object) CServer.messageBuffer.size();
                CServer.messageBuffer.add(msg);
                CServer.REQUEST_PENDING++;
                for (String s : CServer.loginNames) {
                    if (s.equals(newMsge.recipent)) {
                        sendMessage(newMsge, CServer.loginNames.indexOf(s));
                        break;
                    }
                }
                break;
            }
            case Values.FILE_REQUEST_RESPONSE: {
//                System.out.println(msg);
                CServer.REQUEST_PENDING--;
                if (msg.message.equals(Values.FILE_REQUEST_YES)) {
                    int messageBuff = (Integer) msg.fileNumber;
                    Message newMsge = CServer.messageBuffer.get(messageBuff);
                    newMsge.message = "";
                    for (String s : CServer.loginNames) {
                        if (s.equals(newMsge.recipent)) {
                            sendMessage(newMsge, CServer.loginNames.indexOf(s));
                            break;
                        }
                    }
                }
                if (CServer.REQUEST_PENDING == 0) {
                    CServer.messageBuffer.clear();
                }
                break;
            }
            case Values.DISCONNECT_PROTOCOL: {
                CServer.removeClient(msg.sender);
                Message a = new Message(Values.OBJECTTYPE_LIST_PROTOCOL, msg.sender, Values.SERVER_USER_NAME, CServer.loginNames);
                updateLists(a);
                this.cont = false;
                break;
            }
            case Values.LOGIN_PROTOCOL: {
                String userTemp = msg.sender;
                char[] passtemp = (char[]) msg.obMessage;
                User tempUser = new User(userTemp, passtemp);
                Message newMsge;
                Message chatHistory;
                Message message = new Message(Values.CONNECTIN_PROTOCOL, "");
                message.message = msg.sender;
                for (User checkUser : CServer.users) {
                    if (tempUser.isCorrect(checkUser)) {
                        CServer.addClient(userTemp, obout);
                        newMsge = new Message(Values.LOGIN_RESPONSE_PROTOCOL, msg.sender, Values.SERVER_USER_NAME, Values.LOGIN_RESPONSE_PROTOCOL_YES);
                       
                        if (getSenderIndex(newMsge.recipent) < CServer.loginNames.size()) {
                            sendMessage(newMsge, CServer.loginNames.indexOf(msg.sender));
                            //////////////////*/*/********
                            loadChat(msg.sender);
                           
                        }

                        Message a = new Message(Values.OBJECTTYPE_LIST_PROTOCOL, msg.sender, Values.SERVER_USER_NAME, CServer.loginNames);
                        updateLists(a);
                        return;
                    }
                }
                newMsge = new Message(Values.LOGIN_RESPONSE_PROTOCOL, msg.sender, Values.SERVER_USER_NAME, "");
                try {
                    obout.writeObject(newMsge);
                    obout.close();
                    obout.close();
                    obin.close();
                    this.cont = false;
                } catch (IOException ex) {
                    Logger.getLogger(AcceptClient.class.getName()).log(Level.SEVERE, null, ex);
                }

                if (getSenderIndex(newMsge.recipent) < CServer.loginNames.size()) {
                    sendMessage(msg, getSenderIndex(newMsge.recipent));
                }
                break;
            }
            case Values.SIGN_UP_PROTOCOL: {
                String userTemp = msg.sender;
                char[] passtemp = (char[]) msg.obMessage;
                User tempUser = new User(userTemp, passtemp);
                Message newMsge;
                if (!userExist(tempUser)) {
                    newMsge = new Message(Values.SIGN_UP_RESPONSE_PROTCOL, msg.sender, Values.SERVER_USER_NAME, Values.SIGN_UP_RESPONSE_PROTCOL_DONE);
                    CServer.users.add(tempUser);
                    CServer.saveUsers();
                    sendMessage(newMsge, getSenderIndex(msg.sender));
                } else {
                    newMsge = new Message(Values.SIGN_UP_RESPONSE_PROTCOL, msg.sender, Values.SERVER_USER_NAME, "");
                }
                Message message = new Message(Values.LOGIN_PROTOCOL, "", msg.sender, passtemp);
                recMessage(message);
                if (getSenderIndex(newMsge.recipent) < CServer.loginNames.size()) {
                    sendMessage(msg, getSenderIndex(newMsge.recipent));
                }
                break;
            }
            default: {
                break;
            }
        }
    }

    private int getSenderIndex(String userName) {
        for (String s : CServer.loginNames) {
            if (s.equals(userName)) {
                return CServer.loginNames.indexOf(s);
            }
        }
        return CServer.loginNames.size() + 1;
    }


    private void loadChat(String userName)
    {
    File chatFile=new File("../ChatHistory/"+userName+".txt");    
    String chat="";
    try
    ( Scanner in=new Scanner(chatFile);)
    
    {
        while(in.hasNextLine())
        {
            chat=chat+in.nextLine()+"\n";
        }

    Message chatHistory=new Message(Values.CHAT_HISTORY_PROTOCOL,userName,Values.SERVER_USER_NAME,chat);    
    sendMessage(chatHistory,CServer.loginNames.indexOf(userName));
    }

    catch(FileNotFoundException e)
    {
        //System.out.println("File path wrong\n../ChatHistory/"+userName+".txt\"");
        System.out.println("No chat History Sent As either FilePath wrong or a new User");


    }

    
    }

    private boolean userExist(User u) {
        for (User us : CServer.users) {
            if (us.isCorrect(u)) {
                return true;
            }
        }
        return false;
    }
}
