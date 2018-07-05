package frontend;

import akka.actor.*;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import frontend.chatgui.ChatGUIActor;
import frontend.data.NewMessageData;
import frontend.message.*;
import scala.concurrent.ExecutionContextExecutor;
import scala.concurrent.duration.Duration;
import utility.NetworkUtility;

import java.util.concurrent.TimeUnit;

import static akka.pattern.PatternsCS.pipe;

public class ChatClientActor extends AbstractActorWithStash {

    private enum Request {NEWCHAT, JOIN, LEAVE}

    private String chatServiceURL = "http://" + NetworkUtility.getLanOrLocal() + ":" + NetworkUtility.CHAT_SERVICE_PORT;
    private final String address;
    private final ActorRef gui;
    private String chat = "";
    private int messageIndex = -1;
    private Request currentRequest = null;

    final Http http = Http.get(context().system());
    final ExecutionContextExecutor dispatcher = context().dispatcher();
    private final ObjectMapper jsonMapper = new ObjectMapper();

    public ChatClientActor(String address) {
        this.address = address;
        gui = getContext().actorOf(Props.create(ChatGUIActor.class));
    }

    public static Props props(String address) {
        return Props.create(ChatClientActor.class, () -> new ChatClientActor(address));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConnectRequestMessage.class, req -> {
                    currentRequest = Request.JOIN;
                    pipe(http.singleRequest(HttpRequest.POST(
                            chatServiceURL + "/chats/" + req.getName() + "/users/" + address))
                            , dispatcher).to(self());
                    chat = req.getName();
                    getContext().setReceiveTimeout(Duration.create(5, TimeUnit.SECONDS));
                }).match(LeaveRequestMessage.class, req -> {
                    currentRequest = Request.LEAVE;
                    pipe(http.singleRequest(HttpRequest.DELETE(
                            chatServiceURL + "/chats/" + req.getName() + "/users/" + address))
                            , dispatcher).to(self());
                    getContext().setReceiveTimeout(Duration.create(5, TimeUnit.SECONDS));
                }).match(NewChatRequestMessage.class, req -> {
                    currentRequest = Request.NEWCHAT;
                    pipe(http.singleRequest(HttpRequest.POST(chatServiceURL + "/chats/" + req.getName() + "/"))
                            , dispatcher).to(self());
                    getContext().setReceiveTimeout(Duration.create(5, TimeUnit.SECONDS));
                }).match(SendMessage.class, msg -> {
                    NewMessageData message = new NewMessageData(chat, address, msg.getMessage());
                    String messageJson = "";
                    try {
                        messageJson = jsonMapper.writeValueAsString(message);
                    } catch (JsonProcessingException e) {
                        e.printStackTrace();
                    }
                    pipe(http.singleRequest(HttpRequest.POST(chatServiceURL + "/chats/" + chat + "/messages").withEntity(
                            HttpEntities.create(ContentTypes.APPLICATION_JSON, messageJson)))
                            , dispatcher).to(self());
                }).match(NextMessage.class, msg -> {
                    if (messageIndex == -1) {
                        messageIndex = msg.getIndex();
                    }
                    if (msg.getIndex() == messageIndex) {
                        messageIndex++;
                        gui.tell(msg, getSelf());
                        unstashAll();
                    } else {
                        stash();
                    }
                })
                .match(HttpResponse.class, response -> {
                    if (currentRequest != null) {
                        getContext().setReceiveTimeout(Duration.Undefined());
                        if (response.status().intValue() == StatusCodes.SERVICE_UNAVAILABLE.intValue()) {
                            if (currentRequest == Request.NEWCHAT) {
                                if (response.entity().toString().length() > 45) {
                                    String responseBody = response.entity().toString().substring(44, response.entity().toString().length() - 1);
                                    gui.tell(new NewChatResponseMessage(false, responseBody), getSelf());
                                }
                            } else if (currentRequest == Request.JOIN) {
                                if (response.entity().toString().length() > 45) {
                                    String responseBody = response.entity().toString().substring(44, response.entity().toString().length() - 1);
                                    gui.tell(new ConnectionResultMessage(false, responseBody), getSelf());
                                }
                            } else if (currentRequest == Request.LEAVE) {
                                System.exit(0);
                            }
                        } else if (response.getHeader("Location").isPresent()) {
                            if (response.getHeader("Location").toString().contains("/users/")) {
                                if (response.status().intValue() == StatusCodes.CREATED.intValue()) {
                                    gui.tell(new ConnectionResultMessage(true, ""), getSelf());
                                } else if (response.status().intValue() == StatusCodes.NO_CONTENT.intValue()) {
                                    System.exit(0);
                                } else {
                                    if (response.entity().toString().length() > 45) {
                                        String responseBody = response.entity().toString().substring(44, response.entity().toString().length() - 1);
                                        gui.tell(new ConnectionResultMessage(false, responseBody), getSelf());
                                    }
                                }
                            } else if (response.getHeader("Location").toString().contains("/messages/")) {
                                //nothing for now
                            } else {
                                boolean success = false;
                                if (response.status().intValue() == StatusCodes.CREATED.intValue()) {
                                    success = true;
                                }
                                if (response.entity().toString().length() > 45) {
                                    String responseBody = response.entity().toString().substring(44, response.entity().toString().length() - 1);
                                    gui.tell(new NewChatResponseMessage(success, responseBody), getSelf());
                                }
                            }
                        }
                        currentRequest = null;
                    }
                }).match(ReceiveTimeout.class, timeout -> {
                    getContext().setReceiveTimeout(Duration.Undefined());
                    if (currentRequest == Request.NEWCHAT) {
                        gui.tell(new NewChatResponseMessage(false, "Unable to connect to server. Please check your connection and try again."), getSelf());
                    } else if (currentRequest == Request.JOIN) {
                        gui.tell(new NewChatResponseMessage(false, "Unable to connect to server. Please check your connection and try again."), getSelf());
                    } else if (currentRequest == Request.LEAVE) {
                        System.exit(0);
                    }
                    currentRequest = null;
                }).build();
    }
}
