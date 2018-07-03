package backend.run.service;

import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.cluster.client.ClusterClient;
import akka.cluster.client.ClusterClientSettings;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.model.*;
import akka.http.javadsl.model.headers.Location;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import akka.util.Timeout;
import backend.microservice.registrymicroservice.data.UserList;
import backend.microservice.registrymicroservice.message.AddUserMessage;
import backend.microservice.registrymicroservice.message.GetUsersMessage;
import backend.microservice.registrymicroservice.message.RemoveUserMessage;
import backend.microservice.registrymicroservice.message.UserListMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import scala.concurrent.duration.FiniteDuration;
import utility.NetworkUtility;

import java.io.File;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;

import static akka.http.javadsl.server.PathMatchers.segment;
import static akka.pattern.PatternsCS.ask;

public class RunRegistryService extends AllDirectives {

    private final ObjectMapper jsonMapper = new ObjectMapper();
    private static String registryMicroserviceUrl;
    private final ActorRef client;

    public static void main(String[] args) throws Exception {
        String ip = NetworkUtility.getLanOrLocal();
        int port = NetworkUtility.findNextAviablePort(ip, NetworkUtility.REGISTRY_CLIENT_PORT);
        System.out.println("Try connection on " + ip + ":" + port);
        Config config = ConfigFactory.parseFile(new File("src/main/resources/min-remote.conf"));
        Config portConfig =
                ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port + "\n" +
                        "akka.remote.netty.tcp.hostname=" + ip).withFallback(config);
        ActorSystem system = ActorSystem.create("RegistryClient", portConfig);

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);

        //In order to access all directives we need an instance where the routes are define.
        RunRegistryService app = new RunRegistryService(system);

        int serverPort = NetworkUtility.findNextAviablePort(ip, NetworkUtility.REGISTRY_MICROSERVICE_PORT);
        registryMicroserviceUrl = "http://" + ip + ":" + serverPort;

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(ip, serverPort), materializer);

        System.out.println("Server online at " + registryMicroserviceUrl + "/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return

        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    private RunRegistryService(final ActorSystem system) {
        client = system.actorOf(ClusterClient.props(
                ClusterClientSettings.create(system).withInitialContacts(
                        NetworkUtility.initialContacts(NetworkUtility.REGISTRY_SYSTEM_NAME,
                                NetworkUtility.REGISTRY_FIRST_PORT))), "client");
    }

    private Route createRoute() {
        return route(pathPrefix("chats", () ->
                path(segment(), (String chatId) ->
                        route(
                                get(() -> {
                                    final Timeout timeout = Timeout.durationToTimeout(
                                            FiniteDuration.apply(10, TimeUnit.SECONDS));
                                    CompletionStage<HttpResponse> httpResponseFuture =
                                            ask(client, new ClusterClient.Send("/system/sharding/" +
                                                            NetworkUtility.REGISTRY_SHARD_REGION_NAME, new GetUsersMessage(chatId), true),
                                                    timeout).thenApply(
                                                    response -> {
                                                        Location locationHeader = Location.create(registryMicroserviceUrl + "/chats/" + chatId + "/users");
                                                        UserList users = new UserList(((UserListMessage) response).getUsers());
                                                        String usersJson = "";
                                                        try {
                                                            usersJson = jsonMapper.writeValueAsString(users);
                                                        } catch (JsonProcessingException e) {
                                                            e.printStackTrace();
                                                        }
                                                        return HttpResponse.create()
                                                                .withStatus(StatusCodes.OK)
                                                                .addHeader(locationHeader)
                                                                .withEntity(
                                                                        HttpEntities.create(ContentTypes.APPLICATION_JSON, usersJson));
                                                    }
                                            );
                                    return completeWithFuture(httpResponseFuture);
                                }),
                                pathPrefix("users", () ->
                                        path(segment(), (String userId) ->
                                                route(
                                                        post(() -> {
                                                            final Timeout timeout = Timeout.durationToTimeout(
                                                                    FiniteDuration.apply(10, TimeUnit.SECONDS));
                                                            CompletionStage<HttpResponse> httpResponseFuture =
                                                                    ask(client, new ClusterClient.Send("/system/sharding/" +
                                                                                    NetworkUtility.REGISTRY_SHARD_REGION_NAME, new AddUserMessage(chatId, userId), true),
                                                                            timeout).thenApply(
                                                                            response -> {
                                                                                Location locationHeader = Location.create(registryMicroserviceUrl + "/chats/" + chatId + "/users/" + userId);
                                                                                return HttpResponse.create()
                                                                                        .withStatus(StatusCodes.CREATED)
                                                                                        .addHeader(locationHeader);
                                                                            }
                                                                    );
                                                            return completeWithFuture(httpResponseFuture);
                                                        }),
                                                        delete(() -> {
                                                            final Timeout timeout = Timeout.durationToTimeout(
                                                                    FiniteDuration.apply(10, TimeUnit.SECONDS));
                                                            CompletionStage<HttpResponse> httpResponseFuture =
                                                                    ask(client, new ClusterClient.Send("/system/sharding/" +
                                                                                    NetworkUtility.REGISTRY_SHARD_REGION_NAME, new RemoveUserMessage(chatId, userId), true),
                                                                            timeout).thenApply(
                                                                            response -> {
                                                                                Location locationHeader = Location.create(registryMicroserviceUrl + "/chats/" + chatId + "/users/" + userId);
                                                                                return HttpResponse.create()
                                                                                        .withStatus(StatusCodes.CREATED)
                                                                                        .addHeader(locationHeader);
                                                                            }
                                                                    );
                                                            return completeWithFuture(httpResponseFuture);
                                                        }))))))));
    }
}