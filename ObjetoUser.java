import javax.swing.*;
import java.awt.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class ObjetoUser extends UnicastRemoteObject implements IUserChat {
    public String serverIP;
	JFrame frame = new JFrame("Chatter");
    JTextField textField = new JTextField(50);
    public JTextArea messageArea = new JTextArea(16, 50);

    public ObjetoUser() throws RemoteException {
        textField.setEditable(false);
        messageArea.setEditable(false);
        frame.getContentPane().add(textField, BorderLayout.SOUTH);
        frame.getContentPane().add(new JScrollPane(messageArea), BorderLayout.CENTER);
        frame.pack();
    }

    public void deliverMsg(String senderName, String msg) throws RemoteException {
        messageArea.append(senderName + ": " + msg + "\n");
    }
}
