import javax.swing.*;

import static java.lang.System.exit;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;

public class UserChat extends ObjetoUser {
    private IServerChat serverStub;
    private IRoomChat roomStub;
    private String clientName;
    private boolean inRoom = false;

    public UserChat() throws RemoteException {
        super();
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

            super.textField.setEditable(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void joinOrCreateRoom(String roomName, UserChat client){
        try{
            if (!roomName.isEmpty()) {
                if (!serverStub.getRooms().contains(roomName)) {
                    serverStub.createRoom(roomName);
                }
                messageArea.setText("");
				roomStub = (IRoomChat) Naming.lookup("//" + this.serverIP + ":2020/" + roomName);
                roomStub.joinRoom(clientName, client);
            }
        }
        catch (RemoteException e) {
            promptSalas();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    } 

    public void clientLeaveRoom(){
        try {
            this.roomStub.leaveRoom(this.clientName);
            this.inRoom = false;
            this.messageArea.setText("");
            this.promptSalas();
        } catch (RemoteException e1) {}
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
                    if (!client.inRoom){
                        client.joinOrCreateRoom(sala, client);
                        client.inRoom = true;
                    }
                    else {                    
                        client.messageArea.append("[SERVIDOR] Tem que sair da sala pra conseguir entrar em uma nova.\n");
                    }
                }
                else if (msg.equals("/leave")){
                    if (client.inRoom){
                        client.clientLeaveRoom();
                    }
                    else{
                        client.messageArea.setText("[SERVIDOR] Tem que estar em uma sala pra conseguir usar esse commando.\n");
                        client.promptSalas();
                    }
                }
                else if (msg.equals("/close")){
                    exit(0);
                }
                else if (msg.equals("/help")){
                    client.messageArea.append("Comandos disponiveis:\n" +
                                        "\t/join [nome da sala]\t\t(Entra na sala especificada)\n" + 
                                        "\t/leave\t\t\t\t(Sai da sala)\n" + 
                                        "\t/close\t\t\t\t(Fecha a janela do chat)\n");
                }
                else if (msg.startsWith("/")){
                    client.messageArea.append("Comando nao reconhecido! Digite /help para obter a lista de comandos\n");
                }
                else {
                    try {
                        if (client.inRoom){
                            client.roomStub.sendMsg(client.clientName, msg);
                        }
                        else{
                            client.messageArea.append("E preciso entrar em alguma sala para mandar mensagens!\n");
                        }
                    } catch (RemoteException ex) {}
                }
                client.textField.setText("");
            }
        });
    }
}
