package frontend;

import akka.actor.*;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import frontend.chatgui.ChatGUIActor;
import frontend.data.NewMessage;
import frontend.message.*;
import scala.concurrent.ExecutionContextExecutor;
import utility.NetworkUtility;

import static akka.pattern.PatternsCS.pipe;

public class ChatClientActor extends AbstractActorWithStash {

    private String chatServiceURL = "http://" + NetworkUtility.getLanOrLocal() + ":" + NetworkUtility.CHAT_SERVICE_PORT;
    private final String address;
    private final ActorRef gui;
    private String chat = "";

    final Http http = Http.get(context().system());
    final ExecutionContextExecutor dispatcher = context().dispatcher();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public ChatClientActor(String address) {
        this.address = address;
        gui = getContext().actorOf(Props.create(ChatGUIActor.class));
    }

    static Props props(String address) {
        return Props.create(ChatClientActor.class, () -> new ChatClientActor(address));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConnectRequestMessage.class, req -> {
                    pipe(http.singleRequest(HttpRequest.POST(
                            chatServiceURL + "/chats/" + req.getName() + "/users/" + address))
                            , dispatcher).to(self());
                    chat = req.getName();
                }).match(LeaveRequestMessage.class, req -> {
                    pipe(http.singleRequest(HttpRequest.DELETE(
                            chatServiceURL + "/chats/" + req.getName() + "/users/" + address))
                            , dispatcher).to(self());
                }).match(NewChatRequestMessage.class, req -> {
                    pipe(http.singleRequest(HttpRequest.POST(chatServiceURL + "/chats/" + req.getName() + "/"))
                        , dispatcher).to(self());
                }).match(SendMessage.class, msg -> {
                    NewMessage message = new NewMessage(chat, address, msg.getMessage());
                    String messageJson = "";
                    try {
                        messageJson = jsonMapper.writeValueAsString(message);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    pipe(http.singleRequest(HttpRequest.POST(chatServiceURL + "/chats/" + chat + "/messages/").withEntity(
                            HttpEntities.create( ContentTypes.APPLICATION_JSON, messageJson)))
                            , dispatcher).to(self());
                }).match(NextMessage.class, response -> {
                    gui.tell(response, getSelf());
                })
                .match(HttpResponse.class, response -> {
                    if (response.getHeader("Location").isPresent()) {
                        if (response.getHeader("Location").toString().contains("/users/")) {
                            if(response.status().intValue() == StatusCodes.CREATED.intValue()){
                                gui.tell(new ConnectionResultMessage(true, ""), getSelf());
                            } else if(response.status().intValue() == StatusCodes.NO_CONTENT.intValue()){
                                System.exit(0);
                            } else {
                                String responseBody = response.entity().toString().substring(44, response.entity().toString().length() -1);
                                gui.tell(new ConnectionResultMessage(false, responseBody), getSelf());
                            }
                        } else {
                            boolean success = false;
                            if(response.status().intValue() == StatusCodes.CREATED.intValue()){
                                success = true;
                            }
                            String responseBody = response.entity().toString().substring(44, response.entity().toString().length() -1);
                            gui.tell(new NewChatResponseMessage(success, responseBody), getSelf());
                        }
                    }
                })
                .build();
    }

}
