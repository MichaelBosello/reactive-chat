package backend.chatroommicroservice.apiprovider;

import akka.NotUsed;
import akka.actor.*;
import akka.cluster.client.ClusterClient;
import akka.cluster.client.ClusterClientSettings;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.Location;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import backend.chatroommicroservice.cluster.message.ChatCreatedMessage;
import backend.chatroommicroservice.cluster.message.NewChatMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.FiniteDuration;
import utility.NetworkUtility;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.pattern.PatternsCS.ask;
import static akka.http.javadsl.server.PathMatchers.segment;

import java.io.File;

public class RunChatRoomMicroservice extends AllDirectives {

    private static String chatServiceUrl;
    private final ActorRef client;

    public static void main(String[] args) throws Exception {
        String ip = NetworkUtility.getLanOrLocal();
        int port = NetworkUtility.findNextAviablePort(ip, NetworkUtility.CHAT_ROOM_CLIENT_PORT);
        System.out.println("Try connection on " + ip + ":" + port);
        Config config = ConfigFactory.parseFile(new File("src/main/resources/min-remote.conf"));
        Config portConfig =
                ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port + "\n" +
                        "akka.remote.netty.tcp.hostname=" + ip).withFallback(config);
        ActorSystem system = ActorSystem.create("ChatRoomClient", portConfig);

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        //In order to access all directives we need an instance where the routes are define.
        RunChatRoomMicroservice app = new RunChatRoomMicroservice(system);

        int serverPort = NetworkUtility.findNextAviablePort(ip, NetworkUtility.CHAT_ROOM_MICROSERVICE_PORT);
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

    private RunChatRoomMicroservice(final ActorSystem system) {
        client = system.actorOf(ClusterClient.props(
                ClusterClientSettings.create(system).withInitialContacts(
                        NetworkUtility.initialContacts(NetworkUtility.CHAT_ROOM_SYSTEM_NAME,
                                NetworkUtility.CHAT_ROOM_FIRST_PORT))), "client");
    }

    private Route createRoute() {
        return route(pathPrefix("chat", () ->
                path(segment(), (String id) -> route(
        post( () -> {
            final Timeout timeout = Timeout.durationToTimeout(
                    FiniteDuration.apply(10, TimeUnit.SECONDS));
            CompletionStage<HttpResponse> httpResponseFuture =
                    ask(client, new ClusterClient.Send("/system/sharding/ChatRoomShard", new NewChatMessage(id), true), timeout).thenApply(
                            response -> {
                                Location locationHeader = Location.create(chatServiceUrl + "/" + id);
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
        })))));
                /*path(segment(), (String id) -> route(
                        post(() -> {
                            System.out.println("request received");
                            final Timeout timeout = Timeout.durationToTimeout(
                                    FiniteDuration.apply(10, TimeUnit.SECONDS));
                            CompletionStage<HttpResponse> httpResponseFuture =
                                    ask(client, new NewChatMessage(id), timeout).thenApply(
                                    response -> {
                                        Location locationHeader = Location.create(chatServiceUrl + "/" + id);
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
                        })))));*/
    }
}
