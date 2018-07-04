package backend.chatservice;

import akka.cluster.sharding.ShardRegion;
import backend.chatservice.message.*;

public class ChatShardExtractor implements ShardRegion.MessageExtractor {

    private static final int NUMBER_OF_SHARDS = 100;

    /*
    * AddUserMessage
    *
    * RemoveUserMessage
    *
    * NewMessage
    * */

    @Override
    public String entityId(Object message) {
        if (message instanceof NewChatMessage)
            return ((NewChatMessage) message).getId();
        else if (message instanceof AddUserMessage)
            return ((AddUserMessage) message).getChatId();
        else if (message instanceof RemoveUserMessage)
            return ((RemoveUserMessage) message).getChatId();
        else if (message instanceof NewMessage)
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
        String stringId = null;
        if (message instanceof NewChatMessage)
            stringId = ((NewChatMessage) message).getId();
        else if (message instanceof AddUserMessage)
            stringId = ((AddUserMessage) message).getChatId();
        else if (message instanceof RemoveUserMessage)
            stringId = ((RemoveUserMessage) message).getChatId();
        else if (message instanceof NewMessage)
            stringId = ((NewMessage) message).getChatId();

        if (stringId != null) {
            long id = Math.abs(stringId.hashCode());
            return String.valueOf(id % NUMBER_OF_SHARDS);
        } else {
            return null;
        }
    }
}
