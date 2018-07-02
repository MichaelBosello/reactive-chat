package frontend;

import akka.actor.*;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import frontend.chatgui.ChatGUIActor;
import frontend.message.*;
import scala.concurrent.ExecutionContextExecutor;
import utility.NetworkUtility;

import java.util.concurrent.CompletionStage;

import static akka.pattern.PatternsCS.pipe;

public class ChatActor extends AbstractActorWithStash {

    private String chatServiceURL = "http://" + NetworkUtility.getLanOrLocal() + ":" + NetworkUtility.CHAT_ROOM_MICROSERVICE_PORT;
    private final ActorRef gui;

    final Http http = Http.get(context().system());
    final ExecutionContextExecutor dispatcher = context().dispatcher();

    public ChatActor() {
        gui = getContext().actorOf(Props.create(ChatGUIActor.class));
    }

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(ConnectRequestMessage.class, req -> {

                }).match(LeaveRequestMessage.class, req -> {

                }).match(NewChatRequestMessage.class, req -> {
                    pipe(fetch(chatServiceURL + "/chat/" + req.getName()), dispatcher).to(self());
                }).match(SendMessage.class, msg -> {

                }).match(HttpResponse.class, response -> {
                    if (response.getHeader("Location").isPresent()) {
                        if (response.getHeader("Location").toString().contains("/user/")) {

                        } else if (response.getHeader("Location").toString().contains("/message/")) {

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

    CompletionStage<HttpResponse> fetch(String url) {
        return http.singleRequest(HttpRequest.POST(url));
    }
}
