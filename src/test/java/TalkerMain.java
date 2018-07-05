import akka.NotUsed;
import akka.actor.ActorRef;
import akka.actor.ActorSystem;
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
import frontend.ChatClientActor;
import frontend.data.NextMessageData;
import frontend.message.ConnectRequestMessage;
import frontend.message.NewChatRequestMessage;
import frontend.message.NextMessage;
import frontend.message.SendMessage;
import utility.NetworkUtility;

import java.io.File;
import java.util.concurrent.CompletionStage;

import static java.lang.Thread.sleep;


public class TalkerMain extends AllDirectives {

    private final static int N_MESSAGE = 200;
    private final static String CHAT_NAME = "chat";

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

        TalkerMain app = new TalkerMain(system, ip + ":" + serverPort);

        final Flow<HttpRequest, HttpResponse, NotUsed> routeFlow = app.createRoute().flow(system, materializer);
        final CompletionStage<ServerBinding> binding = http.bindAndHandle(routeFlow,
                ConnectHttp.toHost(ip, serverPort), materializer);

        new Thread(() -> {
            try {
                sleep(1000);
                app.client.tell(
                        new NewChatRequestMessage(CHAT_NAME), null);
                sleep(1000);
                app.client.tell(
                        new ConnectRequestMessage(CHAT_NAME), null);
                sleep(20000);//wait for launch another talker
                for (int i = 0; i < N_MESSAGE; i++) {
                    if (i == 150) {
                        app.client.tell(new SendMessage(":enter-cs"), null);
                    }
                    app.client.tell(new SendMessage("##User" + ip + ":" + serverPort + "## message: " + i), null);
                    sleep(200);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();

        System.out.println("Server online at " + chatServiceUrl + "/\nPress RETURN to stop...");
        System.in.read(); // let it run until user presses return

        binding
                .thenCompose(ServerBinding::unbind) // trigger unbinding from the port
                .thenAccept(unbound -> system.terminate()); // and shutdown when done
    }

    public TalkerMain(final ActorSystem system, String address) {
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
