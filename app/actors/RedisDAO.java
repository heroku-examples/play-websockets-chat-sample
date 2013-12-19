package actors;

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.typesafe.plugin.RedisPlugin;
import play.libs.Akka;
import play.libs.Json;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.Set;

/**
 * The RedisDAO Actor is responsible for updating state in Redis and sending & receiving messages to / from Redis
 */
public class RedisDAO extends UntypedActor {

    public static ActorRef actor = Akka.system().actorOf(Props.create(RedisDAO.class));

    final JedisPool jedisPool;

    final Jedis redisSubscribe;
    
    final RedisListener redisListener;

    private static final String CHANNEL = "messages";
    private static final String MEMBERS = "members";
    
    public RedisDAO() {
        jedisPool = play.Play.application().plugin(RedisPlugin.class).jedisPool();

        redisSubscribe = jedisPool.getResource();
        
        redisListener = new RedisListener();

        // don't block on the redis subscribe
        new Thread(new Runnable() {
            @Override
            public void run() {
                redisSubscribe.subscribe(redisListener, CHANNEL);
            }
        }).start();
    }

    @Override
    public void postStop() {
        // clean up the redis listener
        redisListener.unsubscribe();
        jedisPool.returnResource(redisSubscribe);
    }
    
    @Override
    public void onReceive(Object message) throws Exception {

        Jedis jedis = jedisPool.getResource();

        try {
            // check if a given name is available
            if (message instanceof IsNameAvailable) {
                if (jedis.sismember(MEMBERS, ((IsNameAvailable)message).username)) {
                    getSender().tell(new NameNotAvailable(), getSelf());
                }
                else {
                    getSender().tell(new NameIsAvailable(), getSelf());
                }
            }
            else if (message instanceof ChatRoom.Join) {
                String username = ((ChatRoom.Join)message).username;
                
                // add the username to the list of members
                jedis.sadd(MEMBERS, username);

                refreshMembers(jedis);
            }
            else if (message instanceof ChatRoom.Quit) {
                String username = ((ChatRoom.Quit)message).username;

                // remove the username from the list of members
                jedis.srem(MEMBERS, username);

                refreshMembers(jedis);
            }
            else if (message instanceof ChatRoom.Talk) {
                jedis.publish(CHANNEL, Json.stringify(Json.toJson(message)));
            }
        }
        finally {
            jedisPool.returnResource(jedis);
        }
        
    }

    private void refreshMembers(Jedis jedis) {
        Set<String> members = jedis.smembers(MEMBERS);
        jedis.publish(CHANNEL, Json.stringify(Json.toJson(new Members(members))));
    }

    public static class IsNameAvailable {
        final String username;

        public IsNameAvailable(String username) {
            this.username = username;
        }
    }

    public static class NameIsAvailable { }

    public static class NameNotAvailable { }
    
    
    public static class Members {
        public final String type = "members";
        
        public final Set<String> members;
        
        public Members(Set<String> members) {
            this.members = members;
        }
    }
    
    
    public static class RedisListener extends JedisPubSub {
        @Override
        public void onMessage(String channel, String message) {
            // pass this message on to the ChatRoom
            ChatRoom.actor.tell(Json.parse(message), RedisDAO.actor);
        }
        @Override
        public void onPMessage(String arg0, String arg1, String arg2) { }
        @Override
        public void onPSubscribe(String arg0, int arg1) { }
        @Override
        public void onPUnsubscribe(String arg0, int arg1) { }
        @Override
        public void onSubscribe(String arg0, int arg1) { }
        @Override
        public void onUnsubscribe(String arg0, int arg1) { }
    }
    
}
