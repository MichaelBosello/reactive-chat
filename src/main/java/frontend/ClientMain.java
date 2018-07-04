package frontend;

import akka.NotUsed;
import akka.actor.*;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.http.javadsl.marshallers.jackson.Jackson;
import akka.http.javadsl.model.HttpRequest;
import akka.http.javadsl.model.HttpResponse;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.stream.ActorMaterializer;
import akka.stream.javadsl.Flow;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import frontend.data.NextMessageData;
import frontend.message.NextMessage;
import utility.NetworkUtility;

import java.io.File;
import java.util.concurrent.CompletionStage;

import static akka.pattern.PatternsCS.ask;

public class ClientMain extends AllDirectives {

    private static String chatServiceUrl;
    private final ActorRef client;

    public static void main(String[] args) throws Exception {
        String ip = NetworkUtility.getLanOrLocal();
        int port = NetworkUtility.findNextAviablePort(ip, NetworkUtility.CLIENT_CHAT_BASE_PORT);
        System.out.println("Try connection on " + ip + ":" + port);
        Config config = ConfigFactory.parseFile(new File("src/main/resources/min-remote.conf"));
        Config portConfig =
                ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).withFallback(config);
        ActorSystem system = ActorSystem.create("client", portConfig);
        System.out.println("Client on " + ip + ":" + port);

        final Http http = Http.get(system);
        final ActorMaterializer materializer = ActorMaterializer.create(system);
        int serverPort = NetworkUtility.findNextAviablePort(ip, NetworkUtility.CHAT_CLIENT_RECEIVE_PORT);
        chatServiceUrl = "http://" + ip + ":" + serverPort;

        ClientMain app = new ClientMain(system, ip + ":" + serverPort);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(ip, serverPort), materializer);

        System.out.println("Server online at " + chatServiceUrl + "/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return

        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    public ClientMain(final ActorSystem system, String address) {
        client = system.actorOf(ChatClientActor.props(address), "client");
    }

    private Route createRoute() {
        return route(path("nextmessage", () ->
                post(() -> entity(Jackson.unmarshaller(NextMessageData.class), newMessage -> {
                    client.tell(new NextMessage(newMessage.getMessage(), newMessage.getIndex()), ActorRef.noSender());
                    return complete(StatusCodes.ACCEPTED, "message received");
                }))));
    }
}
