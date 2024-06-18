// Trabalho 2 de Sistemas Distribuidos
// Chat com RMI
// Fernando Kalikosque Laydner Júnior
// Miguel Jorge Silva das Virgens
import java.rmi.RemoteException;
import java.util.ArrayList;
 
public interface IServerChat extends java.rmi.Remote {
    public ArrayList<String> getRooms() throws RemoteException;
    public void createRoom(String roomName) throws RemoteException;
}