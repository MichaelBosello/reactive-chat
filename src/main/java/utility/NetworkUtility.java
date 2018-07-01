package utility;

import akka.actor.Address;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;

public class NetworkUtility {

    public final static int CHATSERVICE_FIRST_PORT = 2551;
    public final static int CHAT_ROOM_FIRST_PORT = 2651;
    public final static int REGISTRY_FIRST_PORT = 2751;
    public final static int MESSAGE_MANAGER_FIRST_PORT = 2851;
    public final static int BROKER_FIRST_PORT = 2951;

    public final static int CHATSERVICE_CLIENT_PORT = 3551;
    public final static int CHAT_ROOM_CLIENT_PORT = 3651;
    public final static int REGISTRY_CLIENT_PORT = 3751;
    public final static int MESSAGE_MANAGER_CLIENT_PORT = 3851;
    public final static int BROKER_CLIENT_PORT = 3951;

    private static List<Address> getTwoClusterSeed(int port) {
        List<Address> list = new LinkedList<>();
        list.add(new Address(getLanOrLocal(), String.valueOf(port)));
        list.add(new Address(getLanOrLocal(), String.valueOf(port + 1)));
        return list;
    }

    public static List<Address> getChatClusterSeed() {
        return getTwoClusterSeed(CHATSERVICE_FIRST_PORT);
    }

    public static List<Address> getChatRoomClusterSeed() {
        return getTwoClusterSeed(CHAT_ROOM_FIRST_PORT);
    }

    public static List<Address> getRegistryClusterSeed() {
        return getTwoClusterSeed(REGISTRY_FIRST_PORT);
    }

    public static List<Address> getMessageManagerClusterSeed() {
        return getTwoClusterSeed(MESSAGE_MANAGER_FIRST_PORT);
    }

    public static List<Address> getBrokerClusterSeed() {
        return getTwoClusterSeed(BROKER_FIRST_PORT);
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
                System.out.println("Soket " + port + " already in use, try next one");
                port++;
            } catch (IOException e) {
                portFound = true;
            }
        }
        return port;
    }

    public static String ipPortConcat(String ip, int port) {
        return ip + ":" + port;
    }
}
