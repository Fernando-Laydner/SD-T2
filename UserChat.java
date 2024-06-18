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

    JFrame frame = new JFrame("Chat com RMI");
    JTextField textField = new JTextField(50);
    JEditorPane messageArea = new JEditorPane();
    StringBuilder messageContent = new StringBuilder();
    public JButton bEnviar = new JButton("Enviar");
    public JButton bSair = new JButton("Fechar chat");

    public UserChat() throws RemoteException {
        textField.setEditable(false);
        messageArea.setContentType("text/html");
        messageArea.setEditable(false);
        messageArea.setText("<html><body></body></html>");

        frame.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);

        // Configurar messageArea
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        frame.getContentPane().add(new JScrollPane(messageArea), gbc);

        // Configurar textField
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 2;
        gbc.gridheight = 2; // Alterado para 2 para ocupar duas linhas
        gbc.fill = GridBagConstraints.BOTH; // Alterado para BOTH para ocupar verticalmente também
        gbc.weightx = 0.9;
        gbc.weighty = 0.01; // Alterado para 0.9 para ocupar mais espaço vertical

        frame.getContentPane().add(textField, gbc);

        // Configurar bEnviar
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.gridheight = 1; // Alterado para 1 para ocupar apenas uma linha
        gbc.fill = GridBagConstraints.HORIZONTAL; // Alterado para HORIZONTAL para ocupar apenas horizontalmente
        gbc.weightx = 0.1;
        gbc.weighty = 0.0;

        frame.getContentPane().add(bEnviar, gbc);

        // Configurar bSair
        gbc.gridx = 2;
        gbc.gridy = 2; // Alterado para 2 para posicionar abaixo do botão de enviar
        gbc.gridwidth = 1;
        gbc.gridheight = 1; // Mantido como 1 para ocupar apenas uma linha
        gbc.fill = GridBagConstraints.HORIZONTAL; // Mantido como HORIZONTAL
        gbc.weightx = 0.1;
        gbc.weighty = 0.0;

        frame.getContentPane().add(bSair, gbc);

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

        // Add action listener to textField
        textField.addActionListener(e -> {
            String msg = textField.getText();
            if (!msg.isEmpty()) {
                if (msg.startsWith("/join ")) {
                    String sala = msg.substring(6).trim();
                    joinOrCreateRoom(sala);
                } else if (msg.equals("/leave")) {
                    clientLeaveRoom();
                } else if (msg.equals("/close")) {
                    exit(0);
                } else if (msg.equals("/help")) {
                    appendToMessageArea("Comandos disponiveis:<br>" +
                            "&emsp;/join [nome da sala]&ensp;&ensp;(Entra na sala especificada)<br>" +
                            "&emsp;/leave &emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&ensp;(Sai da sala)<br>" +
                            "&emsp;/close &emsp;&emsp;&emsp;&emsp;&emsp;&emsp;&ensp;(Fecha a janela do chat)<br>");
                } else if (msg.startsWith("/")) {
                    appendToMessageArea("Comando nao reconhecido! Digite /help para obter a lista de comandos<br>");
                } else {
                    sendMessage(msg);
                }
                textField.setText("");
            }
        });

        // Add action listener to bEnviar
        bEnviar.addActionListener(e -> {
            String msg = textField.getText();
            if (!msg.isEmpty()) {
                sendMessage(msg);
                textField.setText("");
            }
        });

        // Add action listener to bSair
        bSair.addActionListener(e -> { 
            if (inRoom){
                clientLeaveRoom();
            }
            else {
                exit(0);
            }});
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
            appendToMessageArea("Selecione a sala desejada ou digite /join [nome da sala] para cria uma nova:<br>");
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
                    messageContent.setLength(0);
                    this.roomStub = (IRoomChat) Naming.lookup("//" + this.serverIP + ":2020/" + roomName);
                    this.roomStub.joinRoom(clientName, (IUserChat) this);
                    bSair.setText("Sair da sala");
                    this.inRoom = true;
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                appendToMessageArea("[SERVIDOR] Erro ao entrar na sala");
                promptSalas();
            } catch (Exception e) {
                e.printStackTrace();
            } 
        } else {
            this.appendToMessageArea("[SERVIDOR] Tem que sair da sala pra conseguir entrar em uma nova.<br>");
        }
    }

    public void clientLeaveRoom() {
        if (this.inRoom) {
            try {
                this.roomStub.leaveRoom(this.clientName);
                this.inRoom = false;
                bSair.setText("Fechar chat");
                messageContent.setLength(0);
                this.promptSalas();
            } catch (RemoteException e1) {
                e1.printStackTrace();
            }
        } else {
            messageContent.setLength(0);
            appendToMessageArea("[SERVIDOR] Tem que estar em uma sala pra conseguir usar esse commando.<br>");
            this.promptSalas();
        }
    }

    public void sendMessage(String msg) {
        try {
            if (this.inRoom) {
                this.roomStub.sendMsg(this.clientName, msg);
            } else {
                messageContent.setLength(0);
                this.appendToMessageArea("[SERVIDOR] Necessario entrar em alguma sala para mandar mensagens!<br>");
                this.promptSalas();
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
    }
}