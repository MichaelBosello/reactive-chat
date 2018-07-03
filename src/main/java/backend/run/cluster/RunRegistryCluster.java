package backend.run.cluster;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.client.ClusterClientReceptionist;
import akka.cluster.sharding.ClusterSharding;
import akka.cluster.sharding.ClusterShardingSettings;
import akka.persistence.cassandra.testkit.CassandraLauncher;
import backend.microservice.registrymicroservice.RegistryActor;
import backend.microservice.registrymicroservice.RegistryShardExtractor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import utility.NetworkUtility;

import java.io.File;

public class RunRegistryCluster {

    private final static int CASSANDRA_PORT = 9042;

    public static void main(String[] args) {
        if(NetworkUtility.findNextAviablePort("localhost", CASSANDRA_PORT) == CASSANDRA_PORT) {
            File cassandraDirectory = new File("target/chat");
            CassandraLauncher.start(cassandraDirectory, CassandraLauncher.DefaultTestConfigResource(), true, CASSANDRA_PORT);
        }
        deployRegistryActor();
    }

    public static void deployRegistryActor() {
        String ip = NetworkUtility.getLanOrLocal();
        int port = NetworkUtility.findNextAviablePort(ip, NetworkUtility.REGISTRY_FIRST_PORT);
        System.out.println("Try connection on " + ip + ":" + port);
        Config config = ConfigFactory.parseFile(new File("src/main/resources/cluster.conf"));
        Config portConfig =
                ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port + "\n" +
                        "akka.remote.netty.tcp.hostname=" + ip).withFallback(config);

        ActorSystem system = ActorSystem.create(NetworkUtility.REGISTRY_SYSTEM_NAME, portConfig);
        final Cluster cluster = Cluster.get(system);
        cluster.joinSeedNodes(NetworkUtility.getClusterSeed(
                NetworkUtility.REGISTRY_SYSTEM_NAME, NetworkUtility.REGISTRY_FIRST_PORT));

        ClusterShardingSettings settings = ClusterShardingSettings.create(system);
        ActorRef shardRegion = ClusterSharding.get(system).start(NetworkUtility.REGISTRY_SHARD_REGION_NAME,
                Props.create(RegistryActor.class), settings, new RegistryShardExtractor());

        ClusterClientReceptionist.get(system).registerService(shardRegion);

        System.out.println("System on " + ip + ":" + port);
    }
}
