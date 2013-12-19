package actors;

import play.libs.Akka;

import scala.concurrent.duration.*;
import akka.actor.*;

import static java.util.concurrent.TimeUnit.*;

public class Robot extends UntypedActor {
    
    public Robot() {
        // Make the robot talk every 30 seconds
        Akka.system().scheduler().schedule(
                Duration.create(30, SECONDS),
                Duration.create(30, SECONDS),
                RedisDAO.actor,
                new ChatRoom.Talk("robot", "I'm still alive"),
                getContext().dispatcher(),
                getSelf()
        );
    }

    @Override
    public void onReceive(Object message) throws Exception {
        unhandled(message);
    }
}
