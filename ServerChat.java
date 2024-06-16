import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;
import java.util.Iterator;
import java.util.Scanner;
 
public class ServerChat extends ObjetoServer {
    public ServerChat() throws RemoteException {
        super();
        createRoom("abacate");
        createRoom("sala");
    }

    public static void main(String[] args) {
        try {
            // Cria e exporta o registro RMI na porta 2020
            LocateRegistry.createRegistry(2020);

            ServerChat server = new ServerChat();
            Naming.rebind("//localhost:2020/Servidor", server);

            @SuppressWarnings("resource")
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
                else if(command.equals("/help")){
                    System.out.println("Comandos disponiveis:\n" +
                                        "\t/create [nome da sala]\t\t(Abre a sala especificada)\n" + 
                                        "\t/close [nome da sala]\t\t(Fecha a sala especificada)\n" + 
                                        "\t/lista\t\t\t\t(Lista o nome das salas)");
                }
                else {
			        System.out.println("Commando nao reconhecido! Digite '/help' para lista de comandos");
			    }
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}
