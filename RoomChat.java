import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;

public class RoomChat extends UnicastRemoteObject implements IRoomChat {
    public Map<String, IUserChat>  userList;
    String nome_sala;

    public RoomChat() throws RemoteException {
        this.userList = new HashMap<String, IUserChat>();
    }
 
    public void sendMsg(String usrName, String msg) throws RemoteException {
        userList.forEach((nome, user) -> {
            try {
                user.deliverMsg(usrName, msg);
            } catch (RemoteException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void joinRoom(String usrName, IUserChat user) throws RemoteException {
        if (checkUserInRoom(usrName)){
            user.deliverMsg("[SERVIDOR]", "Nao e permitido dois usuarios com o mesmo nome: " + usrName);
            throw new RemoteException("Usuario com nome igual ao de outro usuario na sala.");
        }
        userList.put(usrName, user);
        sendMsg("[SALA]", "Bem vindo " + usrName + " a sala: " + this.nome_sala);
    }

    public void leaveRoom(String usrName) throws RemoteException {
        userList.remove(usrName);
    }

    public void closeRoom() throws RemoteException {
        sendMsg("[SALA]", "Sala fechada pelo servidor");
        userList.clear();
    }

    public String getRoomName() throws RemoteException {
        return nome_sala;
    }

    public boolean checkUserInRoom(String usrName) throws RemoteException {
        for (String nome : userList.keySet()){
            if (nome.equals(usrName)){
                return true;
            }
        }
        return false;
    }
}
