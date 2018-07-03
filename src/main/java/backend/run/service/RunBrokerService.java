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
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.model.headers.Location;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import backend.microservice.brokermicroservice.message.SendMessage;
import backend.microservice.messagemanagermicroservice.data.UserMessage;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.FiniteDuration;
import utility.NetworkUtility;

import java.io.File;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.server.PathMatchers.segment;
import static akka.pattern.PatternsCS.ask;

public class RunBrokerService extends AllDirectives {

    private static String brokerMicroserviceUrl;
    private final ActorRef client;

    public static void main(String[] args) throws Exception {
        String ip = NetworkUtility.getLanOrLocal();
        int port = NetworkUtility.findNextAviablePort(ip, NetworkUtility.BROKER_CLIENT_PORT);
        System.out.println("Try connection on " + ip + ":" + port);
        Config config = ConfigFactory.parseFile(new File("src/main/resources/min-remote.conf"));
        Config portConfig =
                ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port + "\n" +
                        "akka.remote.netty.tcp.hostname=" + ip).withFallback(config);
        ActorSystem system = ActorSystem.create("BrokerClient", portConfig);

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        //In order to access all directives we need an instance where the routes are define.
        RunBrokerService app = new RunBrokerService(system);

        int serverPort = NetworkUtility.findNextAviablePort(ip, NetworkUtility.BROKER_MICROSERVICE_PORT);
        brokerMicroserviceUrl = "http://" + ip + ":" + serverPort;

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(ip, serverPort), materializer);

        System.out.println("Server online at " + brokerMicroserviceUrl + "/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return

        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    private RunBrokerService(final ActorSystem system) {
        client = system.actorOf(ClusterClient.props(
                ClusterClientSettings.create(system).withInitialContacts(
                        NetworkUtility.initialContacts(NetworkUtility.BROKER_SYSTEM_NAME,
                                NetworkUtility.BROKER_FIRST_PORT))), "client");
    }

    private Route createRoute() {
        return route(pathPrefix("chats", () ->
                pathPrefix(segment(), (String chatId) -> route(
                        path("send", () ->
                                post(() -> entity(Jackson.unmarshaller(UserMessage.class), userMessage -> {
                                    final Timeout timeout = Timeout.durationToTimeout(
                                            FiniteDuration.apply(10, TimeUnit.SECONDS));
                                    CompletionStage<HttpResponse> httpResponseFuture =
                                            ask(client, new ClusterClient.Send("/system/sharding/" +
                                                            NetworkUtility.BROKER_SHARD_REGION_NAME,
                                                            new SendMessage(chatId, userMessage.getUserId(),
                                                                    userMessage.getMessage(), userMessage.isChatChanged()), true),
                                                    timeout).thenApply(
                                                    response -> {
                                                        Location locationHeader = Location.create(
                                                                brokerMicroserviceUrl + "/chats/" + chatId + "/send/");
                                                        return HttpResponse.create()
                                                                .withStatus(StatusCodes.OK)
                                                                .addHeader(locationHeader);
                                                    }
                                            );
                                    return completeWithFuture(httpResponseFuture);
                                })))))));
    }
}