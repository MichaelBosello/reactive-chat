package backend.microservice.messagemanagermicroservice;

import akka.actor.PoisonPill;
import akka.actor.ReceiveTimeout;
import akka.cluster.sharding.ShardRegion;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpRequest;
import akka.persistence.AbstractPersistentActorWithTimers;
import akka.persistence.SnapshotOffer;
import backend.microservice.messagemanagermicroservice.data.UserMessage;
import backend.microservice.messagemanagermicroservice.message.NewMessage;
import backend.microservice.messagemanagermicroservice.message.TimeoutCS;
import backend.microservice.messagemanagermicroservice.state.MessageState;
import backend.microservice.messagemanagermicroservice.state.UpdateIndexEvent;
import backend.microservice.registrymicroservice.message.SuccessMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;
import utility.NetworkUtility;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static akka.pattern.PatternsCS.pipe;

public class MessageManagerActor extends AbstractPersistentActorWithTimers {

    private String brokerServiceURL = "http://" + NetworkUtility.getLanOrLocal() + ":" + NetworkUtility.BROKER_MICROSERVICE_PORT;

    final Http http = Http.get(context().system());
    final ExecutionContextExecutor dispatcher = context().dispatcher();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    private final static String ENTER_CS_MAGIC = ":enter-cs";
    private final static String EXIT_CS_MAGIC = ":exit-cs";
    private final static int CS_MAX_SECONDS = 10;

    private Map<String, String> chatAndUserInCS = new HashMap<>();
    private MessageState state = new MessageState();
    private static final int SNAP_SHOT_INTERVAL = 1000;

    @Override
    public String persistenceId() {
        return "Manager-" + getSelf().path().name();
    }

    @Override
    public Receive createReceiveRecover() {
        return receiveBuilder()
                .match(UpdateIndexEvent.class, state::update)
                .match(SnapshotOffer.class, snapshotOffer -> state = (MessageState) snapshotOffer.snapshot())
                .matchEquals(ReceiveTimeout.getInstance(), msg -> passivate())
                .build();
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(NewMessage.class, msg -> {
                    if (!chatAndUserInCS.containsKey(msg.getChatId()) ||
                            msg.getUserId().equals(chatAndUserInCS.get(msg.getChatId()))) {


                        UpdateIndexEvent update = new UpdateIndexEvent(msg.getChatId());

                        persist(update, (UpdateIndexEvent e) -> {
                            if (msg.getMessage().equals(ENTER_CS_MAGIC)) {
                                chatAndUserInCS.put(msg.getChatId(), msg.getUserId());
                                timers().startSingleTimer(msg.getChatId(),
                                        new TimeoutCS(msg.getChatId()),
                                        Duration.create(CS_MAX_SECONDS, TimeUnit.SECONDS));
                            }
                            if (msg.getMessage().equals(EXIT_CS_MAGIC)) {
                                chatAndUserInCS.remove(msg.getChatId());
                                timers().cancel(msg.getChatId());
                            }
                            UserMessage message = new UserMessage(msg.getChatId(), msg.getUserId(),
                                    msg.getMessage(), msg.isChatChanged(), state.getIndex(msg.getChatId()));

                            String messageJson = "";
                            try {
                                messageJson = jsonMapper.writeValueAsString(message);
                            } catch (JsonProcessingException ex) {
                                ex.printStackTrace();
                            }
                            pipe(http.singleRequest(HttpRequest.POST(
                                    brokerServiceURL + "/chats/" + msg.getChatId() + "/send")
                                    .withEntity(HttpEntities.create(ContentTypes.APPLICATION_JSON, messageJson))),
                                    dispatcher).to(self());

                            state.update(e);
                            getContext().getSystem().eventStream().publish(e);
                            if (lastSequenceNr() % SNAP_SHOT_INTERVAL == 0 && lastSequenceNr() != 0)
                                // IMPORTANT: create a copy of snapshot because ExampleState is mutable
                                saveSnapshot(state.copy());

                        });
                    }
                    getSender().tell(new SuccessMessage(), getSelf());
                }).match(TimeoutCS.class, timeout -> {
                    getSelf().tell(new NewMessage(timeout.getChatId(),
                            chatAndUserInCS.get(timeout.getChatId()), EXIT_CS_MAGIC, false), getSelf());
                }).build();
    }

    private void passivate() {
        getContext().getParent().tell(
                new ShardRegion.Passivate(PoisonPill.getInstance()), getSelf());
    }
}