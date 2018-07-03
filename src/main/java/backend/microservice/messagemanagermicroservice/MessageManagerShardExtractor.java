package backend.microservice.messagemanagermicroservice;

import akka.cluster.sharding.ShardRegion;
import backend.microservice.messagemanagermicroservice.message.NewMessage;

public class MessageManagerShardExtractor implements ShardRegion.MessageExtractor {

    private static final int NUMBER_OF_SHARDS = 150;

    @Override
    public String entityId(Object message) {
        if (message instanceof NewMessage)
            return ((NewMessage) message).getChatId();
        else
            return null;
    }

    @Override
    public Object entityMessage(Object message) {
        return message;
    }

    @Override
    public String shardId(Object message) {
        if (message instanceof NewMessage) {
            String stringId = ((NewMessage) message).getChatId();
            long id = Math.abs(stringId.hashCode());
            return String.valueOf(id % NUMBER_OF_SHARDS);
        } else {
            return null;
        }
    }
}
