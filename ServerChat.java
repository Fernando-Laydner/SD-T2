import java.rmi.RemoteException;
import static java.lang.System.exit;

import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Scanner;
 
public class ServerChat extends UnicastRemoteObject implements IServerChat {
    ArrayList<String> salas;
    ArrayList<RoomChat> rooms; 

    public ServerChat() throws RemoteException {
        salas = new ArrayList<>();
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
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            System.out.println("Sala " + roomName + " iniciada com sucesso!");
        }
        else{
            System.out.println("Sala " + roomName + " ja existe e nao foi criada.");
        }
    }

    public static void main(String[] args) {
        try {
            // Cria e exporta o registro RMI na porta 2020
            LocateRegistry.createRegistry(2020);

            ServerChat server = new ServerChat();
            Naming.rebind("//localhost:2020/Servidor", server);
            
            String ipAddress = InetAddress.getLocalHost().getHostAddress();
            System.out.println("Endereco IP: " + ipAddress);

            server.createRoom("abacate");
            server.createRoom("sala");
            
			Scanner scanner = new Scanner(System.in);
            while (true) {
			    String command = scanner.nextLine().trim();
			    if (command.startsWith("/create ")) {
			        String roomName = command.substring(8).trim();
			        if (!roomName.isEmpty()) {
                        server.createRoom(roomName);
			        }
			    }
                else if(command.startsWith("/close ")){
                    String roomName = command.substring(7).trim();
			        if (!roomName.isEmpty()) {
                        Iterator<RoomChat> room = server.rooms.iterator();
                        boolean removed = false;
                        while (room.hasNext()){
                            RoomChat sala = room.next();
                            if (sala.nome_sala.equals(roomName)){
                                sala.closeRoom();
                                Naming.unbind("//localhost:2020/" + roomName);
                                server.rooms.remove(sala);
                                server.salas.remove(roomName);
                                removed = true;
                                break;
                            }
                        }
                        if (!removed){
                            System.out.println("A sala " + roomName + " nao existe!");
                        }
                        else{
                            System.out.println("A sala " + roomName + " foi fechada com sucesso!");
                        }
			        }
                }
                else if(command.equals("/lista")){
                    Iterator<String> room = server.salas.iterator();
                    System.out.println("Lista de salas:");
                    while (room.hasNext()){
                        System.out.println("\t-" + room.next());
                    }
                } 
                else if (command.equals("/exit")){
                    scanner.close();
                    exit(0);
                }
                else if(command.equals("/help")){
                    System.out.println("Comandos disponiveis:\n" +
                                        "\t/create [nome da sala]\t\t(Abre a sala especificada)\n" + 
                                        "\t/close [nome da sala]\t\t(Fecha a sala especificada)\n" + 
                                        "\t/lista\t\t\t\t(Lista o nome das salas)\n" + 
                                        "\t/exit\t\t\t\t(Fecha o servidor)\n");
                }
                else {
			        System.out.println("Comando nao reconhecido ou incorreto! Digite '/help' para lista de comandos");
			    }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
