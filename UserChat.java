import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import java.awt.*;
import static java.lang.System.exit;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;

public class UserChat extends UnicastRemoteObject implements IUserChat {
    private IServerChat serverStub;
    private IRoomChat roomStub;
    private String clientName;
    private boolean inRoom = false;
    public String serverIP;

    JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    JEditorPane messageArea = new JEditorPane();
    StringBuilder messageContent = new StringBuilder();

    public UserChat() throws RemoteException {
        textField.setEditable(false);
        messageArea.setContentType("text/html");
        messageArea.setEditable(false);
        messageArea.setText("<html><body></body></html>");
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();
        frame.setSize(600, 400);

        messageArea.addHyperlinkListener(new HyperlinkListener() {
            @Override
            public void hyperlinkUpdate(HyperlinkEvent e) {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    joinOrCreateRoom(e.getDescription());
                }
            }
        });
    }

    public void deliverMsg(String senderName, String msg) throws RemoteException {
        appendToMessageArea(senderName + ": " + msg + "<br>");
    }

    public void appendToMessageArea(String text) {
        messageContent.append(text);
        messageArea.setText("<html><body>" + messageContent.toString() + "</body></html>");
    }

    private String getName() {
        return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    public void promptSalas() {
        ArrayList<String> listaSalas;
        try {
            listaSalas = serverStub.getRooms();
            Iterator<String> sala = listaSalas.iterator();
            appendToMessageArea("Selecione a sala desejada ou digite o nome da sala para cria-la:<br>");
            while (sala.hasNext()) {
                String room = sala.next();
                appendToMessageArea("    -> <a href='" + room + "'>" + room + "</a><br>");
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void connectToServer() throws RemoteException {
        try {
            serverStub = (IServerChat) Naming.lookup("//" + this.serverIP + ":2020/Servidor");

            promptSalas();

            this.textField.setEditable(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void joinOrCreateRoom(String roomName) {
        if (!this.inRoom) {
            try {
                if (!roomName.isEmpty()) {
                    if (!serverStub.getRooms().contains(roomName)) {
                        serverStub.createRoom(roomName);
                    }
                    messageContent.setLength(0); // Clear the current content
                    messageArea.setText("<html><body></body></html>"); // Reset the message area
                    roomStub = (IRoomChat) Naming.lookup("//" + this.serverIP + ":2020/" + roomName);
                    roomStub.joinRoom(this.clientName, this);
                }
            } catch (RemoteException e) {
                promptSalas();
            } catch (Exception e) {
                e.printStackTrace();
            }
            this.inRoom = true;
        } else {
            this.appendToMessageArea("[SERVIDOR] Tem que sair da sala pra conseguir entrar em uma nova.<br>");
        }
    }

    public void clientLeaveRoom() {
        if (this.inRoom) {
            try {
                this.roomStub.leaveRoom(this.clientName);
                this.inRoom = false;
                messageContent.setLength(0); // Clear the current content
                messageArea.setText("<html><body></body></html>"); // Reset the message area
                this.promptSalas();
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        } else {
            messageArea.setText("[SERVIDOR] Tem que estar em uma sala pra conseguir usar esse commando.<br>");
            this.promptSalas();
        }
    }

    public void sendMessage(String msg) {
        try {
            if (this.inRoom) {
                this.roomStub.sendMsg(this.clientName, msg);
            } else {
                this.appendToMessageArea("É preciso entrar em alguma sala para mandar mensagens!<br>");
            }
        } catch (RemoteException ex) {
            this.appendToMessageArea("[Falha no envio] da mensagem: " + msg + "<br>");
        }
    }

    public static void main(String[] args) throws RemoteException {
        if (args.length != 1) {
            System.err.println("Pass the server IP as the sole command line argument");
            return;
        }

        UserChat client = new UserChat();
        client.frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        client.frame.setVisible(true);

        client.clientName = client.getName();
        client.serverIP = args[0];
        client.connectToServer();

        client.textField.addActionListener(e -> {
            String msg = client.textField.getText();
            if (!msg.isEmpty()) {
                if (msg.startsWith("/join ")) {
                    String sala = msg.substring(6).trim();
                    client.joinOrCreateRoom(sala);
                } else if (msg.equals("/leave")) {
                    client.clientLeaveRoom();
                } else if (msg.equals("/close")) {
                    exit(0);
                } else if (msg.equals("/help")) {
                    client.appendToMessageArea("Comandos disponiveis:<br>" +
                            "\t/join [nome da sala]\t(Entra na sala especificada)<br>" +
                            "\t/leave\t\t(Sai da sala)<br>" +
                            "\t/close\t\t(Fecha a janela do chat)<br>");
                } else if (msg.startsWith("/")) {
                    client.appendToMessageArea("Comando não reconhecido! Digite /help para obter a lista de comandos<br>");
                } else {
                    client.sendMessage(msg);
                }
                client.textField.setText("");
            }
        });
    }
}