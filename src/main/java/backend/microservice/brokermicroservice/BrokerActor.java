package backend.microservice.brokermicroservice;

import akka.actor.AbstractActor;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.HttpEntities;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import backend.microservice.brokermicroservice.message.SendMessage;
import backend.microservice.registrymicroservice.data.UserList;
import backend.microservice.registrymicroservice.message.SuccessMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import scala.concurrent.ExecutionContextExecutor;
import utility.NetworkUtility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static akka.pattern.PatternsCS.pipe;

public class BrokerActor extends AbstractActor {

    private String RegistryServiceURL = "http://" + NetworkUtility.getLanOrLocal() + ":" + NetworkUtility.REGISTRY_MICROSERVICE_PORT;

    final Http http = Http.get(context().system());
    final ExecutionContextExecutor dispatcher = context().dispatcher();
    ObjectMapper mapper = new ObjectMapper();

    private final Map<String, Set<String>> chatUsers = new HashMap<>();
    private final Map<String, Set<String>> messagePending = new HashMap<>();
    private final Set<String> waitingNewUserList = new HashSet<>();

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(SendMessage.class, msg -> {
                    if(waitingNewUserList.contains(msg.getChatId())){
                        toPendingList(msg);
                    } else {
                        if (msg.isChatChanged() || !chatUsers.containsKey(msg.getChatId())) {
                            pipe(http.singleRequest(HttpRequest.GET(
                                    RegistryServiceURL + "/chats/" + msg.getChatId() + "/users"))
                                    , dispatcher).to(self());
                            waitingNewUserList.add(msg.getChatId());
                            toPendingList(msg);
                        } else {
                            send(chatUsers.get(msg.getChatId()), msg.getMessage());
                        }
                    }
                    getSender().tell(new SuccessMessage(), getSelf());

                }).match(HttpResponse.class, response -> {
                    System.out.println(response.entity().toString());
                    String jsonResponse = response.entity().toString()
                            .substring(44, response.entity().toString().length() -1);
                    UserList newList = mapper.readValue(jsonResponse, UserList.class);
                    chatUsers.put(newList.getChatId(), newList.getUsers());

                    waitingNewUserList.remove(newList.getChatId());
                    for (String message: messagePending.get(newList.getChatId())) {
                        send(newList.getUsers(), message);
                    }
                    messagePending.remove(newList.getChatId());
                }).build();
    }

    private void toPendingList(SendMessage msg){
        if(messagePending.containsKey(msg.getChatId())){
            messagePending.get(msg.getChatId()).add(msg.getMessage());
        } else {
            Set<String> pending = new HashSet<>();
            pending.add(msg.getMessage());
            messagePending.put(msg.getChatId(), pending);
        }
    }

    private void send(Set<String> users, String message){
        for (String user : users) {
            http.singleRequest(HttpRequest.POST(user + "/nextmessage")
                    .withEntity(HttpEntities.create(
                            ContentTypes.TEXT_PLAIN_UTF8, message)));
        }
    }
}