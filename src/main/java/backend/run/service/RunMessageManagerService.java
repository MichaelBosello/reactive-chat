package backend.run.service;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.client.ClusterClient;
import akka.cluster.client.ClusterClientSettings;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.*;
import akka.http.javadsl.model.headers.Location;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import backend.microservice.messagemanagermicroservice.data.UserMessage;
import backend.microservice.messagemanagermicroservice.message.NewMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.FiniteDuration;
import utility.NetworkUtility;

import java.io.File;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.server.PathMatchers.segment;
import static akka.pattern.PatternsCS.ask;

public class RunMessageManagerService extends AllDirectives {

    private static String messageManagerMicroserviceUrl;
    private final ActorRef client;

    public static void main(String[] args) throws Exception {
        String ip = NetworkUtility.getLanOrLocal();
        int port = NetworkUtility.findNextAviablePort(ip, NetworkUtility.MESSAGE_MANAGER_CLIENT_PORT);
        System.out.println("Try connection on " + ip + ":" + port);
        Config config = ConfigFactory.parseFile(new File("src/main/resources/min-remote.conf"));
        Config portConfig =
                ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port + "\n" +
                        "akka.remote.netty.tcp.hostname=" + ip).withFallback(config);
        ActorSystem system = ActorSystem.create("MessageManagerClient", portConfig);

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        //In order to access all directives we need an instance where the routes are define.
        RunMessageManagerService app = new RunMessageManagerService(system);

        int serverPort = NetworkUtility.findNextAviablePort(ip, NetworkUtility.MESSAGE_MANAGER_MICROSERVICE_PORT);
        messageManagerMicroserviceUrl = "http://" + ip + ":" + serverPort;

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(ip, serverPort), materializer);

        System.out.println("Server online at " + messageManagerMicroserviceUrl + "/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return

        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    private RunMessageManagerService(final ActorSystem system) {
        client = system.actorOf(ClusterClient.props(
                ClusterClientSettings.create(system).withInitialContacts(
                        NetworkUtility.initialContacts(NetworkUtility.MESSAGE_MANAGER_SYSTEM_NAME,
                                NetworkUtility.MESSAGE_MANAGER_FIRST_PORT))), "client");
    }

    private Route createRoute() {
        return route(pathPrefix("chats", () ->
                pathPrefix(segment(), (String chatId) -> route(
                        path("messages", () ->
                                post(() -> entity(Jackson.unmarshaller(UserMessage.class), userMessage -> {
                                    final Timeout timeout = Timeout.durationToTimeout(
                                            FiniteDuration.apply(10, TimeUnit.SECONDS));
                                    CompletionStage<HttpResponse> httpResponseFuture =
                                            ask(client, new ClusterClient.Send("/system/sharding/" +
                                                            NetworkUtility.MESSAGE_MANAGER_SHARD_REGION_NAME,
                                                            new NewMessage(chatId, userMessage.getUserId(),
                                                                    userMessage.getMessage(), userMessage.isChatChanged()), true),
                                                    timeout).thenApply(
                                                    response -> {
                                                        Location locationHeader = Location.create(
                                                                messageManagerMicroserviceUrl + "/chats/" + chatId + "/messages/");
                                                        return HttpResponse.create()
                                                                .withStatus(StatusCodes.OK)
                                                                .addHeader(locationHeader);
                                                    }
                                            );
                                    return completeWithFuture(httpResponseFuture);
                                })))))));
    }
}