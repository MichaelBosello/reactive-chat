package backend.chatservice;

import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;
import backend.chatservice.state.ChatRoomState;
import backend.chatservice.state.DeleteChatEvent;
import backend.chatservice.state.NewChatEvent;
import backend.chatservice.message.NewChatMessage;

public class ChatRoomActor extends AbstractPersistentActor {

    private ChatRoomState state = new ChatRoomState();
    private int snapShotInterval = 1000;

    @Override
    public String persistenceId() { return "room-" + getSelf().path().name(); }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(NewChatEvent.class, state::update)
                .match(DeleteChatEvent.class, state::update)
                .match(SnapshotOffer.class, snapshotOffer -> state = (ChatRoomState) snapshotOffer.snapshot())
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(NewChatMessage.class, c -> {
                    final NewChatEvent evt = new NewChatEvent(String.valueOf(state.size()));
                    persist(evt, (NewChatEvent e) -> {
                        state.update(e);
                        getContext().getSystem().eventStream().publish(e);
                        if (lastSequenceNr() % snapShotInterval == 0 && lastSequenceNr() != 0)
                            // IMPORTANT: create a copy of snapshot because ExampleState is mutable
                            saveSnapshot(state.copy());
                    });
                })
                .build();
    }
}