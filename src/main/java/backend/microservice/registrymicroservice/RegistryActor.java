package backend.microservice.registrymicroservice;

import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;
import backend.microservice.registrymicroservice.message.*;
import backend.microservice.registrymicroservice.state.AddUserEvent;
import backend.microservice.registrymicroservice.state.RegistryState;
import backend.microservice.registrymicroservice.state.RemoveUserEvent;

public class RegistryActor extends AbstractPersistentActor {

    private RegistryState state = new RegistryState();
    private int snapShotInterval = 1000;

    @Override
    public String persistenceId() { return "registry-" + getSelf().path().name(); }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(AddUserEvent.class, state::update)
                .match(RemoveUserEvent.class, state::update)
                .match(SnapshotOffer.class, snapshotOffer -> state = (RegistryState) snapshotOffer.snapshot())
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(AddUserMessage.class, msg -> {
                    final AddUserEvent evt = new AddUserEvent(msg.getChatId(), msg.getUserId());
                    persist(evt, (AddUserEvent e) -> {
                        state.update(e);
                        getSender().tell(new SuccessMessage(msg.getChatId(), msg.getChatId()), getSelf());
                        getContext().getSystem().eventStream().publish(e);
                        if (lastSequenceNr() % snapShotInterval == 0 && lastSequenceNr() != 0)
                            // IMPORTANT: create a copy of snapshot because ExampleState is mutable
                            saveSnapshot(state.copy());
                    });
                }).match(RemoveUserMessage.class, msg -> {
                    final RemoveUserEvent evt = new RemoveUserEvent(msg.getChatId(), msg.getUserId());
                    persist(evt, (RemoveUserEvent e) -> {
                        state.update(e);
                        getSender().tell(new SuccessMessage(msg.getChatId(), msg.getChatId()), getSelf());
                        getContext().getSystem().eventStream().publish(e);
                        if (lastSequenceNr() % snapShotInterval == 0 && lastSequenceNr() != 0)
                            // IMPORTANT: create a copy of snapshot because ExampleState is mutable
                            saveSnapshot(state.copy());
                    });
                }).match(GetUsersMessage.class, msg -> {
                    getSender().tell(new UserListMessage(msg.getChatId(), state.getUsers(msg.getChatId())), getSelf());
                })
                .build();
    }
}