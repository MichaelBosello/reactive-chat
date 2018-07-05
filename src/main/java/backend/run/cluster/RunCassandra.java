package backend.run.cluster;

import akka.persistence.cassandra.testkit.CassandraLauncher;
import utility.NetworkUtility;

import java.io.File;

public class RunCassandra {

    private final static int CASSANDRA_PORT = 9042;

    public static void main(String[] args) throws Exception{
        if(NetworkUtility.findNextAviablePort("localhost", CASSANDRA_PORT) == CASSANDRA_PORT) {
            File cassandraDirectory = new File("target/chat");
            CassandraLauncher.start(cassandraDirectory, CassandraLauncher.DefaultTestConfigResource(), true, CASSANDRA_PORT);

            System.out.println("Cassandra started\nPress RETURN to stop...");
            System.in.read(); // let it run until user presses return
        }
    }
}
