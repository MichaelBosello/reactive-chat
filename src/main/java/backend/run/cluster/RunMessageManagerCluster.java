package backend.run.cluster;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.client.ClusterClientReceptionist;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.persistence.cassandra.testkit.CassandraLauncher;
import backend.chatservice.ChatActor;
import backend.chatservice.ChatShardExtractor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import utility.NetworkUtility;

import java.io.File;

public class RunMessageManagerCluster {

    private final static int CASSANDRA_PORT = 9042;

    public static void main(String[] args) {
        if(NetworkUtility.findNextAviablePort("localhost", CASSANDRA_PORT) == CASSANDRA_PORT) {
            File cassandraDirectory = new File("target/chat");
            CassandraLauncher.start(cassandraDirectory, CassandraLauncher.DefaultTestConfigResource(), true, CASSANDRA_PORT);
        }
        deployMessageManagerActor();
    }

    public static void deployMessageManagerActor() {
        String ip = NetworkUtility.getLanOrLocal();
        int port = NetworkUtility.findNextAviablePort(ip, NetworkUtility.MESSAGE_MANAGER_FIRST_PORT);
        System.out.println("Try connection on " + ip + ":" + port);
        Config config = ConfigFactory.parseFile(new File("src/main/resources/cluster.conf"));
        Config portConfig =
                ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port + "\n" +
                        "akka.remote.netty.tcp.hostname=" + ip).withFallback(config);

        ActorSystem system = ActorSystem.create(NetworkUtility.MESSAGE_MANAGER_SYSTEM_NAME, portConfig);
        final Cluster cluster = Cluster.get(system);
        cluster.joinSeedNodes(NetworkUtility.getClusterSeed(
                NetworkUtility.MESSAGE_MANAGER_SYSTEM_NAME, NetworkUtility.MESSAGE_MANAGER_FIRST_PORT));

        ClusterShardingSettings settings = ClusterShardingSettings.create(system);
        ActorRef shardRegion = ClusterSharding.get(system).start(NetworkUtility.MESSAGE_MANAGER_SHARD_REGION_NAME,
                Props.create(ChatActor.class), settings, new ChatShardExtractor());

        ClusterClientReceptionist.get(system).registerService(shardRegion);

        System.out.println("System on " + ip + ":" + port);
    }
}
