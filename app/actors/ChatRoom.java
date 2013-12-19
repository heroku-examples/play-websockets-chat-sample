package actors;

import java.util.HashMap;
import java.util.Map;

import akka.actor.Props;
import play.libs.Akka;
import play.mvc.WebSocket;
import akka.actor.ActorRef;
import akka.actor.UntypedActor;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The ChatRoom Actor is responsible for keeping track of the users connected to this node and bridging messages
 * between Redis and those users
 */
public class ChatRoom extends UntypedActor {

    public static ActorRef actor = Akka.system().actorOf(Props.create(ChatRoom.class));

    // Users connected to this node
    Map<String, ActorRef> users = new HashMap<String, ActorRef>();
    
    public ChatRoom() {
        //add the robot
        ActorRef robot = getContext().actorOf(Props.create(Robot.class));
        connect("robot", robot);
    }
    
    private void connect(String username, ActorRef actor) {
        users.put(username, actor);
        RedisDAO.actor.tell(new Join(username), getSelf());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        
        if (message instanceof Connect) {
            Connect connect = (Connect)message;
            
            // create the user actor
            ActorRef user = getContext().actorOf(Props.create(new User.UserCreator(connect.username, connect.in, connect.out)));
            connect(connect.username, user);
        }
        else if (message instanceof Quit) {
            String username = ((Quit)message).username;
            // shut down the user actor
            getContext().stop(users.get(username));
            // remove the user from this node's registry
            users.remove(username);
            // tell redis that this user has quit
            RedisDAO.actor.tell(new Quit(username), getSelf());
        }
        else if (message instanceof JsonNode) {
            // this message is coming from Redis so just send it on to the child
            for (ActorRef child : getContext().getChildren()) {
                child.tell(message, getSelf());
            }
        }
        
    }
    
    // -- Messages
    
    public static class Connect {
        
        final String username;
        final WebSocket.In<JsonNode> in;
        final WebSocket.Out<JsonNode> out;

        public Connect(String username, WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
            this.username = username;
            this.in = in;
            this.out = out;
        }
    }

    public static class Talk {

        public final String type = "talk";

        public final String username;
        public final String message;

        public Talk(String username, String message) {
            this.message = message;
            this.username = username;
        }

    }

    public static class JoinOrQuit {

        public final String username;

        public JoinOrQuit(String username) {
            this.username = username;
        }
    }
    
    public static class Join extends JoinOrQuit {
        public Join(String username) {
            super(username);
        }
    }
    
    public static class Quit extends JoinOrQuit {
        public Quit(String username) {
            super(username);
        }
    }
    
}
