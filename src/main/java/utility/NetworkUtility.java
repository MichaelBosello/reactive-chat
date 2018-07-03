package utility;

import akka.actor.ActorPath;
import akka.actor.ActorPaths;
import akka.actor.Address;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.*;

public class NetworkUtility {

    public final static int CHAT_SERVICE_FIRST_PORT = 2551;
    public final static int REGISTRY_FIRST_PORT = 2751;
    public final static int MESSAGE_MANAGER_FIRST_PORT = 2851;
    public final static int BROKER_FIRST_PORT = 2951;

    public final static int CLIENT_CHAT_BASE_PORT = 3001;

    public final static int CHAT_SERVICE_CLIENT_PORT = 5651;
    public final static int REGISTRY_CLIENT_PORT = 5751;
    public final static int MESSAGE_MANAGER_CLIENT_PORT = 5851;
    public final static int BROKER_CLIENT_PORT = 5951;

    public final static int CHAT_SERVICE_PORT = 8551;
    public final static int REGISTRY_MICROSERVICE_PORT = 8751;
    public final static int MESSAGE_MANAGER_MICROSERVICE_PORT = 8851;
    public final static int BROKER_MICROSERVICE_PORT = 8951;

    public final static String CHAT_SERVICE_SYSTEM_NAME = "ChatRoom";
    public final static String CHAT_SERVICE_SHARD_REGION_NAME = CHAT_SERVICE_SYSTEM_NAME + "Shard";
    public final static String REGISTRY_SYSTEM_NAME = "Registry";
    public final static String REGISTRY_SHARD_REGION_NAME = CHAT_SERVICE_SYSTEM_NAME + "Shard";
    public final static String MESSAGE_MANAGER_SYSTEM_NAME = "MessageManager";
    public final static String MESSAGE_MANAGER_SHARD_REGION_NAME = MESSAGE_MANAGER_SYSTEM_NAME + "Shard";
    public final static String BROKER_SYSTEM_NAME = "Broker";
    public final static String BROKER_SHARD_REGION_NAME = BROKER_SYSTEM_NAME + "Shard";

    public static List<Address> getTwoClusterSeed(String system, int port) {
        List<Address> list = new LinkedList<>();
        list.add(new Address("akka.tcp", system, getLanOrLocal(), port));
        list.add(new Address("akka.tcp", system, getLanOrLocal(), port + 1));
        return list;
    }

    public static Set<ActorPath> initialContacts(String system, int port) {
        return new HashSet<ActorPath>(Arrays.asList(
                ActorPaths.fromString("akka.tcp://" + system + "@" + getLanOrLocal() + ":" + port + "/system/receptionist"),
                ActorPaths.fromString("akka.tcp://" + system + "@" + getLanOrLocal() + ":" + (port + 1) + "/system/receptionist")));
    }

    public static String getLANIP() {
        String ip = null;
        try {
            ip = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        return ip;
    }

    public static String getLanOrLocal() {
        return getLANIP() != null ? NetworkUtility.getLANIP() : "127.0.0.1";
    }

    public static int findNextAviablePort(String ip, int port) {
        boolean portFound = false;
        while (!portFound) {
            try {
                Socket clientSocket = new Socket(ip, port);
                clientSocket.close();
                System.out.println("Socket " + port + " already in use, try next one");
                port++;
            } catch (IOException e) {
                portFound = true;
            }
        }
        return port;
    }
}
