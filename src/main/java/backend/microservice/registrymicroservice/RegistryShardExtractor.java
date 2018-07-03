package backend.microservice.registrymicroservice;

import akka.cluster.sharding.ShardRegion;
import backend.microservice.registrymicroservice.message.AddUserMessage;
import backend.microservice.registrymicroservice.message.GetUsersMessage;
import backend.microservice.registrymicroservice.message.RemoveUserMessage;

public class RegistryShardExtractor implements ShardRegion.MessageExtractor {

    private static final int NUMBER_OF_SHARDS = 50;

    @Override
    public String entityId(Object message) {
        if (message instanceof AddUserMessage)
            return ((AddUserMessage) message).getChatId();
        else if (message instanceof RemoveUserMessage) {
            return ((RemoveUserMessage) message).getChatId();
        } else if (message instanceof GetUsersMessage) {
            return ((GetUsersMessage) message).getChatId();
        } else
            return null;
    }

    @Override
    public Object entityMessage(Object message) {
        return message;
    }

    @Override
    public String shardId(Object message) {
        if (message instanceof AddUserMessage) {
            String stringId = ((AddUserMessage) message).getChatId();
            long id = Math.abs(stringId.hashCode());
            return String.valueOf(id % NUMBER_OF_SHARDS);
        } else if (message instanceof RemoveUserMessage) {
            String stringId = ((RemoveUserMessage) message).getChatId();
            long id = Math.abs(stringId.hashCode());
            return String.valueOf(id % NUMBER_OF_SHARDS);
        } else if (message instanceof GetUsersMessage) {
            String stringId = ((GetUsersMessage) message).getChatId();
            long id = Math.abs(stringId.hashCode());
            return String.valueOf(id % NUMBER_OF_SHARDS);
        } else {
            return null;
        }
    }
}
