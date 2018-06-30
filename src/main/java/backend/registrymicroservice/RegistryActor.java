package backend.registrymicroservice;

import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;
import backend.registrymicroservice.message.AddUserMessage;
import backend.registrymicroservice.state.AddUserEvent;
import backend.registrymicroservice.state.RegistryState;
import backend.registrymicroservice.state.RemoveUserEvent;

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
                .match(AddUserMessage.class, c -> {
                    final AddUserEvent evt = new AddUserEvent(c.getChatId(), c.getUserId());
                    persist(evt, (AddUserEvent e) -> {
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