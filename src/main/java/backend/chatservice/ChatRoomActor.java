package backend.chatservice;

import akka.actor.AbstractActor;

public class ChatRoomActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()

                .build();
    }
}