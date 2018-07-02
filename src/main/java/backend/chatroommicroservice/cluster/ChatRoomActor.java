package backend.chatroommicroservice.cluster;

import akka.actor.PoisonPill;
import akka.actor.ReceiveTimeout;
import akka.cluster.sharding.ShardRegion;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;
import backend.chatroommicroservice.cluster.message.ChatAlreadyExistMessage;
import backend.chatroommicroservice.cluster.message.ChatCreatedMessage;
import backend.chatroommicroservice.cluster.state.ChatRoomState;
import backend.chatroommicroservice.cluster.state.NewChatEvent;
import backend.chatroommicroservice.cluster.message.NewChatMessage;

import java.time.Duration;

public class ChatRoomActor extends AbstractPersistentActor {

    private ChatRoomState state = new ChatRoomState();
    private static final int SNAP_SHOT_INTERVAL = 1000;

    @Override
    public String persistenceId() { return "Room-" + getSelf().path().name(); }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        getContext().setReceiveTimeout(Duration.ofSeconds(600));
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(NewChatEvent.class, state::update)
                .match(SnapshotOffer.class, snapshotOffer -> state = (ChatRoomState) snapshotOffer.snapshot())
                .matchEquals(ReceiveTimeout.getInstance(), msg -> passivate())
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(NewChatMessage.class, msg -> {
                    if (state.getRoom().contains(msg.getId())) {
                        getSender().tell(new ChatAlreadyExistMessage(msg.getId()), getSelf());
                    } else {
                        final NewChatEvent evt = new NewChatEvent(msg.getId());
                        persist(evt, (NewChatEvent e) -> {
                            state.update(e);
                            getContext().getSystem().eventStream().publish(e);
                            if (lastSequenceNr() % SNAP_SHOT_INTERVAL == 0 && lastSequenceNr() != 0)
                                // IMPORTANT: create a copy of snapshot because ExampleState is mutable
                                saveSnapshot(state.copy());
                            getSender().tell(new ChatCreatedMessage(e.getId()), getSelf());
                        });
                    }

                })
                .build();
    }

    private void passivate() {
        getContext().getParent().tell(
                new ShardRegion.Passivate(PoisonPill.getInstance()), getSelf());
    }
}