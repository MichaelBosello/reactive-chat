package backend.microservice.brokermicroservice;

import akka.cluster.sharding.ShardRegion;
import backend.microservice.brokermicroservice.message.SendMessage;

public class BrokerShardExtractor implements ShardRegion.MessageExtractor {

    private static final int NUMBER_OF_SHARDS = 150;

    @Override
    public String entityId(Object message) {
        if (message instanceof SendMessage)
            return ((SendMessage) message).getChatId();
        else
            return null;
    }

    @Override
    public Object entityMessage(Object message) {
        return message;
    }

    @Override
    public String shardId(Object message) {
        if (message instanceof SendMessage) {
            String stringId = ((SendMessage) message).getChatId();
            long id = Math.abs(stringId.hashCode());
            return String.valueOf(id % NUMBER_OF_SHARDS);
        } else {
            return null;
        }
    }
}
