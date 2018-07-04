package backend.run.service;

import akka.NotUsed;
import akka.actor.*;
import akka.cluster.client.ClusterClient;
import akka.cluster.client.ClusterClientSettings;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.Location;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import backend.chatservice.data.NewMessageData;
import backend.chatservice.message.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.FiniteDuration;
import utility.NetworkUtility;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.server.PathMatchers.neutral;
import static akka.pattern.PatternsCS.ask;
import static akka.http.javadsl.server.PathMatchers.segment;

import java.io.File;

public class RunChatService extends AllDirectives {

    private static String chatServiceUrl;
    private final ActorRef client;

    public static void main(String[] args) throws Exception {
        String ip = NetworkUtility.getLanOrLocal();
        int port = NetworkUtility.findNextAviablePort(ip, NetworkUtility.CHAT_SERVICE_CLIENT_PORT);
        System.out.println("Try connection on " + ip + ":" + port);
        Config config = ConfigFactory.parseFile(new File("src/main/resources/min-remote.conf"));
        Config portConfig =
                ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port + "\n" +
                        "akka.remote.netty.tcp.hostname=" + ip).withFallback(config);
        ActorSystem system = ActorSystem.create("ChatRoomClient", portConfig);

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        //In order to access all directives we need an instance where the routes are define.
        RunChatService app = new RunChatService(system);

        int serverPort = NetworkUtility.findNextAviablePort(ip, NetworkUtility.CHAT_SERVICE_PORT);
        chatServiceUrl = "http://" + ip + ":" + serverPort;

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(ip, serverPort), materializer);

        System.out.println("Server online at " + chatServiceUrl + "/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return

        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    private RunChatService(final ActorSystem system) {
        client = system.actorOf(ClusterClient.props(
                ClusterClientSettings.create(system).withInitialContacts(
                        NetworkUtility.initialContacts(NetworkUtility.CHAT_SERVICE_SYSTEM_NAME,
                                NetworkUtility.CHAT_SERVICE_FIRST_PORT))), "client");
    }

    private Route createRoute() {
        return route(pathPrefix("chats", () ->
                pathPrefix(segment(), (String chatId) -> route(
                        path(neutral(), () -> route(
                                post(() -> {
                                    final Timeout timeout = Timeout.durationToTimeout(
                                            FiniteDuration.apply(10, TimeUnit.SECONDS));
                                    CompletionStage<HttpResponse> httpResponseFuture =
                                            ask(client, new ClusterClient.Send("/system/sharding/" +
                                                            NetworkUtility.CHAT_SERVICE_SHARD_REGION_NAME,
                                                            new NewChatMessage(chatId), true), timeout).thenApply(
                                                    response -> {
                                                        Location locationHeader = Location.create(chatServiceUrl + "/chats/" + chatId);
                                                        if (response instanceof ChatCreatedMessage) {
                                                            return HttpResponse.create()
                                                                    .withStatus(StatusCodes.CREATED)
                                                                    .addHeader(locationHeader)
                                                                    .withEntity(((ChatCreatedMessage) response).getId());
                                                        } else {
                                                            return HttpResponse.create()
                                                                    .withStatus(StatusCodes.CONFLICT)
                                                                    .addHeader(locationHeader)
                                                                    .withEntity("Name already taken");
                                                        }
                                                    }
                                            );
                                    return completeWithFuture(httpResponseFuture);
                                }))),
                        path("messages", () ->
                                post(() -> entity(Jackson.unmarshaller(NewMessageData.class), newMessage -> {
                                    final Timeout timeout = Timeout.durationToTimeout(
                                            FiniteDuration.apply(10, TimeUnit.SECONDS));
                                    CompletionStage<HttpResponse> httpResponseFuture =
                                            ask(client, new ClusterClient.Send("/system/sharding/" +
                                                            NetworkUtility.CHAT_SERVICE_SHARD_REGION_NAME,
                                                            new NewMessage(chatId, newMessage.getUserId(),
                                                                    newMessage.getMessage()), true),
                                                    timeout).thenApply(
                                                    response -> {
                                                        Location locationHeader = Location.create(
                                                                chatServiceUrl + "/chats/" + chatId + "/messages/");
                                                        if(response instanceof SuccessMessage) {
                                                            return HttpResponse.create()
                                                                    .withStatus(StatusCodes.OK)
                                                                    .addHeader(locationHeader);
                                                        } else {
                                                            return HttpResponse.create()
                                                                    .withStatus(StatusCodes.NOT_FOUND)
                                                                    .addHeader(locationHeader);
                                                        }
                                                    }
                                            );
                                    return completeWithFuture(httpResponseFuture);
                                }))),
                        pathPrefix("users", () -> route(
                                path(segment(), (String userId) -> route(
                                        post(() -> {
                                            final Timeout timeout = Timeout.durationToTimeout(
                                                    FiniteDuration.apply(10, TimeUnit.SECONDS));
                                            CompletionStage<HttpResponse> httpResponseFuture =
                                                    ask(client, new ClusterClient.Send("/system/sharding/" +
                                                                    NetworkUtility.CHAT_SERVICE_SHARD_REGION_NAME,
                                                                    new AddUserMessage(chatId, userId), true),
                                                            timeout).thenApply(
                                                            response -> {
                                                                Location locationHeader = Location.create(
                                                                        chatServiceUrl + "/chats/" + chatId + "/users/" + userId);
                                                                if(response instanceof SuccessMessage) {
                                                                    return HttpResponse.create()
                                                                            .withStatus(StatusCodes.CREATED)
                                                                            .addHeader(locationHeader);
                                                                } else {
                                                                    return HttpResponse.create()
                                                                            .withStatus(StatusCodes.NOT_FOUND)
                                                                            .addHeader(locationHeader)
                                                                            .withEntity("Chat not found");
                                                                }
                                                            }
                                                    );
                                            return completeWithFuture(httpResponseFuture);
                                        }),
                                        delete(() -> {
                                            final Timeout timeout = Timeout.durationToTimeout(
                                                    FiniteDuration.apply(10, TimeUnit.SECONDS));
                                            CompletionStage<HttpResponse> httpResponseFuture =
                                                    ask(client, new ClusterClient.Send("/system/sharding/" +
                                                                    NetworkUtility.CHAT_SERVICE_SHARD_REGION_NAME,
                                                                    new RemoveUserMessage(chatId, userId), true),
                                                            timeout).thenApply(
                                                            response -> {
                                                                Location locationHeader = Location.create(
                                                                        chatServiceUrl + "/chats/" + chatId + "/users/" + userId);
                                                                if(response instanceof SuccessMessage) {
                                                                    return HttpResponse.create()
                                                                            .withStatus(StatusCodes.NO_CONTENT)
                                                                            .addHeader(locationHeader);
                                                                } else {
                                                                    return HttpResponse.create()
                                                                            .withStatus(StatusCodes.NOT_FOUND)
                                                                            .addHeader(locationHeader)
                                                                            .withEntity("Chat not found");
                                                                }
                                                            }
                                                    );
                                            return completeWithFuture(httpResponseFuture);
                                        })))))))));
    }
}
