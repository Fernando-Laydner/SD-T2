import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.rmi.NotBoundException;

public class UserChat2 extends UnicastRemoteObject implements IUserChat {
    private String userName;
    private IServerChat server;
    private IRoomChat currentRoom;
    private String serverAddress;
    private JTextArea chatArea;
    private JTextField messageField;
    private JComboBox<String> roomComboBox;
    private JFrame frame;

    protected UserChat2(String userName, String serverAddress) throws RemoteException {
        this.userName = userName;
        this.serverAddress = serverAddress;
        initializeUI();
    }

    @Override
    public void deliverMsg(String senderName, String msg) throws RemoteException {
        chatArea.append('[' + senderName + "]: " + msg + "\n");
    }

    public void connectToServer() throws Exception {
        Registry registry = LocateRegistry.getRegistry(serverAddress, 2020);
        server = (IServerChat) registry.lookup("Servidor");
        listRooms();
    }

    public void listRooms() throws RemoteException {
        ArrayList<String> rooms = server.getRooms();
        roomComboBox.removeAllItems();
        for (String room : rooms) {
            roomComboBox.addItem(room);
        }
    }

    public void joinRoom(String roomName) {
        try {
            Registry registry = LocateRegistry.getRegistry(serverAddress, 2020);
            currentRoom = (IRoomChat) registry.lookup(roomName);
            currentRoom.joinRoom(userName, this);
            chatArea.append("Entrou na sala: " + roomName + "\n");
        } catch (NotBoundException e) {
            chatArea.append("Sala não encontrada: " + roomName + "\n");
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void createRoom(String roomName) throws RemoteException {
        server.createRoom(roomName);
        listRooms();
    }

    public void sendMessage(String msg) throws RemoteException {
        if (currentRoom != null) {
            currentRoom.sendMsg(userName, msg);
        } else {
            chatArea.append("Você não está em uma sala.\n");
        }
    }

    public void leaveRoom() throws RemoteException {
        if (currentRoom != null) {
            currentRoom.leaveRoom(userName);
            currentRoom = null;
            chatArea.append("Saiu da sala.\n");
        } else {
            chatArea.append("Você não está em uma sala.\n");
        }
    }

    private void initializeUI() {
        frame = new JFrame("Sala de Chat");
        frame.setSize(400, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        chatArea = new JTextArea();
        chatArea.setEditable(false);
        JScrollPane chatScrollPane = new JScrollPane(chatArea);
        frame.add(chatScrollPane, BorderLayout.CENTER);

        messageField = new JTextField();
        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String message = messageField.getText();
                    if (!message.trim().isEmpty()) {
                        try {
                            sendMessage(message);
                            messageField.setText("");
                        } catch (RemoteException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        });
        frame.add(messageField, BorderLayout.SOUTH);

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BorderLayout());

        roomComboBox = new JComboBox<>();
        roomComboBox.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    listRooms();
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });
        topPanel.add(roomComboBox, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 4));

        JButton joinButton = new JButton("ENTRAR");
        joinButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String roomName = (String) roomComboBox.getSelectedItem();
                if (roomName != null) {
                    joinRoom(roomName);
                }
            }
        });
        buttonPanel.add(joinButton);

        JButton createButton = new JButton("CRIAR");
        createButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String roomName = JOptionPane.showInputDialog(frame, "Digite o nome da nova sala:");
                if (roomName != null && !roomName.trim().isEmpty()) {
                    try {
                        createRoom(roomName);
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        buttonPanel.add(createButton);

        JButton leaveButton = new JButton("SAIR");
        leaveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    leaveRoom();
                } catch (RemoteException ex) {
                    ex.printStackTrace();
                }
            }
        });
        buttonPanel.add(leaveButton);

        JButton sendButton = new JButton("ENVIAR");
        sendButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String message = messageField.getText();
                if (!message.trim().isEmpty()) {
                    try {
                        sendMessage(message);
                        messageField.setText("");
                    } catch (RemoteException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });
        buttonPanel.add(sendButton);

        topPanel.add(buttonPanel, BorderLayout.SOUTH);

        frame.add(topPanel, BorderLayout.NORTH);

        frame.setVisible(true);
    }

    public static void main(String[] args) {
        try {
            String userName = JOptionPane.showInputDialog("Digite seu nome de usuário:");
            String serverAddress = JOptionPane.showInputDialog("Digite o endereço IP do servidor:");

                System.out.println("Tentando conectar a sala!");
            if (userName != null && serverAddress != null && !userName.trim().isEmpty() && !serverAddress.trim().isEmpty()) {
                UserChat2 userChat = new UserChat2(userName, serverAddress);
                userChat.connectToServer();
                System.out.println("Conectou a sala!");
            }else{
                System.out.println("Algo de errado!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
