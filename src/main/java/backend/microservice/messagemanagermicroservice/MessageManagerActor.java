package backend.microservice.messagemanagermicroservice;

import akka.actor.AbstractActor;
import akka.actor.ReceiveTimeout;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpRequest;
import backend.microservice.messagemanagermicroservice.data.UserMessage;
import backend.microservice.messagemanagermicroservice.message.NewMessage;
import backend.microservice.registrymicroservice.message.SuccessMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;
import utility.NetworkUtility;

import java.util.concurrent.TimeUnit;

import static akka.pattern.PatternsCS.pipe;

public class MessageManagerActor extends AbstractActor {

    private String brokerServiceURL = "http://" + NetworkUtility.getLanOrLocal() + ":" + NetworkUtility.BROKER_MICROSERVICE_PORT;

    final Http http = Http.get(context().system());
    final ExecutionContextExecutor dispatcher = context().dispatcher();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    private final static String ENTER_CS_MAGIC = ":enter-cs";
    private final static String EXIT_CS_MAGIC = ":exit-cs";
    private final static int CS_MAX_SECONDS = 10;

    private boolean inCS = false;
    private String userInCS = "";

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(NewMessage.class, msg -> {
                    if(!inCS || (inCS && msg.getUserId().equals(userInCS))) {
                        if (msg.getMessage().equals(ENTER_CS_MAGIC)) {
                            inCS = true;
                            userInCS = msg.getUserId();
                            getContext().setReceiveTimeout(Duration.create(CS_MAX_SECONDS, TimeUnit.SECONDS));
                        }
                        if (msg.getMessage().equals(EXIT_CS_MAGIC)) {
                            inCS = false;
                            getContext().setReceiveTimeout(Duration.Undefined());
                        }

                        UserMessage message = new UserMessage(msg.getChatId(), msg.getUserId(),
                                msg.getMessage(), msg.isChatChanged());

                        String messageJson = "";
                        try {
                            messageJson = jsonMapper.writeValueAsString(message);
                        } catch (JsonProcessingException e) {
                            e.printStackTrace();
                        }
                        pipe(http.singleRequest(HttpRequest.POST(brokerServiceURL + "/chats/" + msg.getChatId() + "/send")
                                        .withEntity(HttpEntities.create(
                                                ContentTypes.APPLICATION_JSON, messageJson)))
                                , dispatcher).to(self());
                    }
                    getSender().tell(new SuccessMessage(), getSelf());
                }).match(ReceiveTimeout.class, timeout -> {
                    inCS = false;
                    getContext().setReceiveTimeout(Duration.Undefined());
                }).build();
    }
}