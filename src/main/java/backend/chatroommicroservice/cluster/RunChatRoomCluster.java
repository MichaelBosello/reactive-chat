package backend.chatroommicroservice.cluster;

import akka.actor.*;
import akka.cluster.Cluster;
import akka.cluster.client.ClusterClientReceptionist;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import utility.NetworkUtility;

import java.io.File;

public class RunChatRoomCluster {

    public static void main(String[] args) {
        deployChatRoomActor();
    }

    public static void deployChatRoomActor() {
        String ip = NetworkUtility.getLanOrLocal();
        int port = NetworkUtility.findNextAviablePort(ip, NetworkUtility.CHAT_ROOM_FIRST_PORT);
        System.out.println("Try connection on " + ip + ":" + port);
        Config config = ConfigFactory.parseFile(new File("src/main/resources/cluster.conf"));
        Config portConfig =
                ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port + "\n" +
                        "akka.remote.netty.tcp.hostname=" + ip).withFallback(config);

        ActorSystem system = ActorSystem.create("ChatRoom", portConfig);
        final Cluster cluster = Cluster.get(system);
        cluster.joinSeedNodes(NetworkUtility.getChatRoomClusterSeed());

        ClusterShardingSettings settings = ClusterShardingSettings.create(system);
        ActorRef shardRegion = ClusterSharding.get(system).start("ChatroomShard",
                Props.create(ChatRoomActor.class), settings, new ChatRoomShardExtractor());

        ClusterClientReceptionist.get(system).registerService(shardRegion);

        System.out.println("System on " + ip + ":" + port);
    }
}
