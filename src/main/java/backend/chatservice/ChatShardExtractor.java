package backend.chatservice;

import akka.cluster.sharding.ShardRegion;
import backend.chatservice.message.NewChatMessage;

public class ChatShardExtractor implements ShardRegion.MessageExtractor {

    private static final int NUMBER_OF_SHARDS = 100;

    @Override
    public String entityId(Object message) {
        if (message instanceof NewChatMessage)
            return ((NewChatMessage) message).getId();
        else
            return null;
    }

    @Override
    public Object entityMessage(Object message) {
        return message;
    }

    @Override
    public String shardId(Object message) {
        if (message instanceof NewChatMessage) {
            String stringId = ((NewChatMessage) message).getId();
            long id = Math.abs(stringId.hashCode());
            return String.valueOf(id % NUMBER_OF_SHARDS);
        } else {
            return null;
        }
    }
}
