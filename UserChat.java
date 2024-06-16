import javax.swing.*;
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
    public JTextArea messageArea = new JTextArea(16, 50);

    public UserChat() throws RemoteException {
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();
    }

    public void deliverMsg(String senderName, String msg) throws RemoteException {
        messageArea.append(senderName + ": " + msg + "\n");
    }

    private String getName() {
        return JOptionPane.showInputDialog(frame, "Choose a screen name:", "Screen name selection",
                JOptionPane.PLAIN_MESSAGE);
    }

    public void promptSalas(){
        ArrayList<String> listaSalas;
		try {
			listaSalas = serverStub.getRooms();
            Iterator<String> sala = listaSalas.iterator();
            messageArea.append("Selecione a sala desejada ou digite o nome da sala para cria-la:\n");
            while (sala.hasNext()) {
                messageArea.append("-> " + sala.next() + "\n");
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

    public void joinOrCreateRoom(String roomName){
        if (!this.inRoom){
            try{
                if (!roomName.isEmpty()) {
                    if (!serverStub.getRooms().contains(roomName)) {
                        serverStub.createRoom(roomName);
                    }
                    messageArea.setText("");
                    roomStub = (IRoomChat) Naming.lookup("//" + this.serverIP + ":2020/" + roomName);
                    roomStub.joinRoom(this.clientName, this);
                }
            }
            catch (RemoteException e) {
                promptSalas();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            this.inRoom = true;
        }
        else {                    
            this.messageArea.append("[SERVIDOR] Tem que sair da sala pra conseguir entrar em uma nova.\n");
        }
    } 

    public void clientLeaveRoom(){
        if (this.inRoom){
            try {
                this.roomStub.leaveRoom(this.clientName);
                this.inRoom = false;
                this.messageArea.setText("");
                this.promptSalas();
            } catch (RemoteException e1) {}
        }
        else{
            this.messageArea.setText("[SERVIDOR] Tem que estar em uma sala pra conseguir usar esse commando.\n");
            this.promptSalas();
        }
    }

    public void sendMessage(String msg){
        try {
            if (this.inRoom){
                this.roomStub.sendMsg(this.clientName, msg);
            }
            else{
                this.messageArea.append("E preciso entrar em alguma sala para mandar mensagens!\n");
            }
        } catch (RemoteException ex) {
            this.messageArea.append("[Falha no envio] da mensagem:" + msg + "\n");
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
                    String sala = msg.substring(5).trim();
                    client.joinOrCreateRoom(sala);
                }
                else if (msg.equals("/leave")){
                    client.clientLeaveRoom();
                }
                else if (msg.equals("/close")){
                    exit(0);
                }
                else if (msg.equals("/help")){
                    client.messageArea.append("Comandos disponiveis:\n" +
                                        "\t/join [nome da sala]\t(Entra na sala especificada)\n" + 
                                        "\t/leave\t\t(Sai da sala)\n" + 
                                        "\t/close\t\t(Fecha a janela do chat)\n");
                }
                else if (msg.startsWith("/")){
                    client.messageArea.append("Comando nao reconhecido! Digite /help para obter a lista de comandos\n");
                }
                else {
                    client.sendMessage(msg);
                }
                client.textField.setText("");
            }
        });
    }
}
