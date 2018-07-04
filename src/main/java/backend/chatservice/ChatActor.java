package backend.chatservice;

import akka.actor.PoisonPill;
import akka.actor.ReceiveTimeout;
import akka.cluster.sharding.ShardRegion;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpRequest;
import akka.persistence.AbstractPersistentActor;
import akka.persistence.SnapshotOffer;
import backend.chatservice.data.UserMessage;
import backend.chatservice.message.*;
import backend.chatservice.state.ChatState;
import backend.chatservice.state.NewChatEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import utility.NetworkUtility;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class ChatActor extends AbstractPersistentActor {

    private String registryServiceURL = "http://" + NetworkUtility.getLanOrLocal() + ":" + NetworkUtility.REGISTRY_MICROSERVICE_PORT;
    private String messageManagerServiceURL = "http://" + NetworkUtility.getLanOrLocal() + ":" + NetworkUtility.MESSAGE_MANAGER_MICROSERVICE_PORT;
    final Http http = Http.get(context().system());
    private final ObjectMapper jsonMapper = new ObjectMapper();

    private ChatState state = new ChatState();
    private static final int SNAP_SHOT_INTERVAL = 1000;

    private final Set<String> changedChat = new HashSet<>();

    @Override
    public String persistenceId() {
        return "Chat-" + getSelf().path().name();
    }

    @Override
    public void preStart() throws Exception {
        super.preStart();
        getContext().setReceiveTimeout(Duration.ofSeconds(600));
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(NewChatEvent.class, state::update)
                .match(SnapshotOffer.class, snapshotOffer -> state = (ChatState) snapshotOffer.snapshot())
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

                }).match(AddUserMessage.class, msg -> {
                    if (state.getRoom().contains(msg.getChatId())) {
                        changedChat.add(msg.getChatId());
                        http.singleRequest(HttpRequest.POST(
                                registryServiceURL + "/chats/" + msg.getChatId() + "/users/" + msg.getUserId()));
                        getSender().tell(new SuccessMessage(), getSelf());
                    } else {
                        getSender().tell(new ErrorMessage("Chat not found"), getSelf());
                    }
                }).match(RemoveUserMessage.class, msg -> {
                    if (state.getRoom().contains(msg.getChatId())) {
                        changedChat.add(msg.getChatId());
                        http.singleRequest(HttpRequest.DELETE(
                                registryServiceURL + "/chats/" + msg.getChatId() + "/users/" + msg.getUserId()));
                        getSender().tell(new SuccessMessage(), getSelf());
                    } else {
                        getSender().tell(new ErrorMessage("Chat not found"), getSelf());
                    }
                }).match(NewMessage.class, msg -> {
                    if (state.getRoom().contains(msg.getChatId())) {
                        boolean changed = false;
                        if(changedChat.contains(msg.getChatId())){
                            changed = true;
                            changedChat.remove(msg.getChatId());
                        }

                        UserMessage message = new UserMessage(msg.getChatId(), msg.getUserId(),
                                msg.getMessage(), changed);

                        String messageJson = "";
                        try {
                            messageJson = jsonMapper.writeValueAsString(message);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        http.singleRequest(HttpRequest.POST(
                                messageManagerServiceURL + "/chats/" + msg.getChatId() + "/messages")
                                .withEntity(HttpEntities.create(
                                        ContentTypes.APPLICATION_JSON, messageJson)));
                        getSender().tell(new SuccessMessage(), getSelf());
                    } else {
                        getSender().tell(new ErrorMessage("Chat not found"), getSelf());
                    }
                }).build();
    }

    private void passivate() {
        getContext().getParent().tell(
                new ShardRegion.Passivate(PoisonPill.getInstance()), getSelf());
    }
}