import java.rmi.RemoteException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;

public class ObjetoServer extends UnicastRemoteObject implements IServerChat {
    ArrayList<String> salas;
    ArrayList<RoomChat> rooms; 

    public ObjetoServer() throws RemoteException {
        salas = new ArrayList<String>();
        rooms = new ArrayList<>();
    }

    public ArrayList<String> getRooms() throws RemoteException {
        return salas;
    }

    public void createRoom(String roomName) throws RemoteException {
        if (!salas.contains(roomName) && !roomName.equals("Servidor")){
            salas.add(roomName);
            RoomChat room = new RoomChat();
            room.nome_sala = roomName;
            try {
                Naming.rebind("//localhost:2020/" + roomName, room);
                rooms.add(room);
            } catch (RemoteException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            System.out.println("Sala " + roomName + " iniciada com sucesso!");
        }
        else{
            System.out.println("Sala " + roomName + " ja existe e nao foi criada.");
        }
    }
}
