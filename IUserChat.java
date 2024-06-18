// Trabalho 2 de Sistemas Distribuidos
// Chat com RMI
// Fernando Kalikosque Laydner Júnior
// Miguel Jorge Silva das Virgens
import java.rmi.RemoteException;
 
public interface IUserChat extends java.rmi.Remote {
    public void deliverMsg(String senderName, String msg) throws RemoteException;
}