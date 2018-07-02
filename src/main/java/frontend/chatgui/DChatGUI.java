package frontend.chatgui;

import utility.NetworkUtility;

import javax.swing.*;
import java.awt.*;
import java.util.HashSet;
import java.util.Set;

public class DChatGUI extends JFrame implements ChatGUI {

    private final Set<ChatObserver> guiObserver = new HashSet<>();
    private final JTextArea chat;
    private final JTextField registryHost;
    private final JTextField message;
    private final JButton joinButton;
    private final JButton newChatButton;
    private final JButton sendButton;
    private boolean joined = false;

    public DChatGUI() {
        this.setTitle("Distributed Chat");
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                notifyLeave();
            }
        });
        this.setLayout(new BorderLayout());

        chat = new JTextArea();
        chat.setEditable(false);
        JScrollPane chatArea = new JScrollPane(chat);
        this.getContentPane().add(chatArea, BorderLayout.CENTER);

        JPanel connectionPanel = new JPanel();
        connectionPanel.add(new JLabel("Chat room name:"));
        registryHost = new JTextField();
        registryHost.setPreferredSize(new Dimension(250, 25));
        connectionPanel.add(registryHost);

        newChatButton = new JButton("Create new room");

        joinButton = new JButton("Join room");
        joinButton.addActionListener( (e) -> {
            if(!joined) {
                joinButton.setText("Connection");
                joinButton.setEnabled(false);
                registryHost.setEditable(false);
                newChatButton.setEnabled(false);
                notifyJoin(registryHost.getText());
            }else{
                notifyLeave();
            }
        });
        connectionPanel.add(joinButton);

        newChatButton.addActionListener( (e) -> {
            newChatButton.setEnabled(false);
            joinButton.setEnabled(false);
            registryHost.setEditable(false);
            notifyNewChatRequest(registryHost.getText());
        });
        connectionPanel.add(newChatButton);

        this.getContentPane().add(connectionPanel,BorderLayout.PAGE_START);

        JPanel messagePanel = new JPanel();
        message = new JTextField();
        message.setPreferredSize(new Dimension(500, 25));
        messagePanel.add(message);
        sendButton = new JButton("Send");
        sendButton.addActionListener( (e) -> {
            notifySend(message.getText());
            message.setText("");
        });
        sendButton.setEnabled(false);
        messagePanel.add(sendButton);
        this.getContentPane().add(messagePanel,BorderLayout.PAGE_END);

        this.getRootPane().setDefaultButton(sendButton);

        this.setSize(1000, 1000);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    @Override
    public void newMessage(String message) {
        chat.append(message + "\n");
    }

    @Override
    public void connected() {
        joined = true;
        joinButton.setText("Leave room");
        joinButton.setEnabled(true);
        sendButton.setEnabled(true);
    }

    @Override
    public void connectionError(String error) {
        JOptionPane.showMessageDialog(null, error, "Connection error", JOptionPane.INFORMATION_MESSAGE);
        joinButton.setText("Join room");
        joinButton.setEnabled(true);
        registryHost.setEditable(true);
        newChatButton.setEnabled(true);
    }

    @Override
    public void newChatCreated() {
        JOptionPane.showMessageDialog(null, "", "Room created and ready to join", JOptionPane.INFORMATION_MESSAGE);
        joinButton.setEnabled(true);
    }

    @Override
    public void newChatError(String error) {
        JOptionPane.showMessageDialog(null, error, "Can't create the specified room", JOptionPane.INFORMATION_MESSAGE);
        joinButton.setEnabled(true);
        registryHost.setEditable(true);
        newChatButton.setEnabled(true);
    }

    @Override
    public void addObserver(ChatObserver observer){
        this.guiObserver.add(observer);
    }

    private void notifyJoin(String host){
        for (final ChatObserver observer : this.guiObserver){
            observer.joinEvent(host);
        }
    }

    private void notifyLeave() {
        for (final ChatObserver observer : this.guiObserver) {
            observer.leaveEvent();
        }
    }

    private void notifyNewChatRequest(String name){
        for (final ChatObserver observer : this.guiObserver){
            observer.newChatEvent(name);
        }
    }

    private void notifySend(String message){
        for (final ChatObserver observer : this.guiObserver){
            observer.sendEvent(message);
        }
    }
}
