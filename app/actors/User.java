package actors;

import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.fasterxml.jackson.databind.JsonNode;
import play.libs.F;
import play.mvc.WebSocket;

/**
 * The User Actor is responsible for sending and receiving messages on the WebSocket
 */
public class User extends UntypedActor {
    
    final String username;
    final WebSocket.In<JsonNode> in;
    final WebSocket.Out<JsonNode> out;
    
    public User(String username, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
        this.username = username;
        this.in = in;
        this.out = out;
        
        // send all message that come into the WebSocket to this actor
        in.onMessage(new F.Callback<JsonNode>() {
            @Override
            public void invoke(JsonNode jsonNode) throws Throwable {
                getSelf().tell(jsonNode, getSelf());
            }
        });
    }
    
    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof JsonNode) {
            JsonNode jsonNode = (JsonNode)message;
            
            if (jsonNode.has("text")) {
                // we received a talk message from the client, lets just send it to redis
                RedisDAO.actor.tell(new ChatRoom.Talk(username, jsonNode.get("text").textValue()), getSelf());
            }
            else {
                // this is an outgoing message
                out.write(jsonNode);
            }
        }
    }

    static class UserCreator implements Creator<User> {

        final String username;
        final WebSocket.In<JsonNode> in;
        final WebSocket.Out<JsonNode> out;

        public UserCreator(String username, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
            this.username = username;
            this.in = in;
            this.out = out;
        }
        
        @Override public User create() {
            return new User(username, in, out);
        }
    }
    
}
