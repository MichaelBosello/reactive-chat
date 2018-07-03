package backend.microservice.messagemanagermicroservice;

import akka.actor.AbstractActor;
import backend.microservice.messagemanagermicroservice.message.NewMessage;

public class MessageManagerActor extends AbstractActor {

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(NewMessage.class, msg -> {

                })
                .build();
    }
}