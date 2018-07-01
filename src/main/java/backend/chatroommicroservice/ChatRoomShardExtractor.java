package backend.chatroommicroservice;

import akka.cluster.sharding.ShardRegion;
import backend.chatroommicroservice.message.NewChatMessage;

public class ChatRoomShardExtractor implements ShardRegion.MessageExtractor {

    private static final int NUMBER_OF_SHARDS = 100;

    @Override
    public String entityId(Object message) {
        if (message instanceof NewChatMessage)
            return String.valueOf(((NewChatMessage) message).getId());
        else
            return null;
    }

    @Override
    public Object entityMessage(Object message) {
        if (message instanceof NewChatMessage)
            return (message);
        else
            return null;
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
