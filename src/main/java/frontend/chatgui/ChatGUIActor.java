package frontend.chatgui;

import akka.actor.AbstractActor;
import frontend.message.*;

import javax.swing.*;

public class ChatGUIActor extends AbstractActor {

    private final ChatGUI gui;

    public ChatGUIActor() {
        gui = new DChatGUI();
        gui.addObserver(new ChatObserver() {
            @Override
            public void joinEvent(String name) {
                getContext().parent().tell(new ConnectRequestMessage(name), getSelf());
            }

            @Override
            public void leaveEvent(String name) {
                getContext().parent().tell(new LeaveRequestMessage(name), getSelf());
            }

            @Override
            public void newChatEvent(String name) {
                getContext().parent().tell(new NewChatRequestMessage(name), getSelf());
            }

            @Override
            public void sendEvent(String message) {
                getContext().parent().tell(new SendMessage(message), getSelf());
            }
        });
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder().match(NextMessage.class, msg -> {
            SwingUtilities.invokeLater(() -> {
                gui.newMessage(msg.getMessage());
            });
        }).match(ConnectionResultMessage.class, msg -> {
            if (msg.isSuccess()) {
                SwingUtilities.invokeLater(gui::connected);
            } else {
                SwingUtilities.invokeLater(() -> gui.connectionError(msg.getError()));
            }
        }).match(NewChatResponseMessage.class, msg -> {
            if (msg.isSuccess()) {
                SwingUtilities.invokeLater(gui::newChatCreated);
            } else {
                SwingUtilities.invokeLater(() -> gui.newChatError(msg.getError()));
            }
        }).build();
    }
}
