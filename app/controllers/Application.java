package controllers;

import actors.ChatRoom;
import actors.RedisDAO;
import akka.dispatch.Mapper;
import play.libs.HttpExecution;
import play.mvc.Http;
import scala.concurrent.Future;
import views.html.chatRoom;
import views.html.index;

import play.libs.Akka;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.WebSocket;

import static akka.pattern.Patterns.ask;
import akka.actor.ActorRef;

import com.fasterxml.jackson.databind.JsonNode;

public class Application extends Controller {
    
    /**
     * Display the home page.
     */
    public static Result index() {
        return ok(index.render());
    }
  
    /**
     * Display the chat room.
     */
    public static F.Promise<Result> chatRoom(final String username) {
        if(username == null || username.trim().equals("")) {
            flash("error", "Please choose a valid username.");
            return F.Promise.pure((Result)redirect(controllers.routes.Application.index()));
        }
        
        // check if username is available
        Future<Object> response = ask(RedisDAO.actor, new RedisDAO.IsNameAvailable(username), 5000);

        Future<Result> resultFuture = response.map(new Mapper<Object, Result>() {
            @Override
            public Result apply(Object message) {
                if (message instanceof RedisDAO.NameIsAvailable) {
                    return ok(chatRoom.render(username));
                }
                else {
                    flash("error", "The username '" + username + "' is taken");
                    return redirect(controllers.routes.Application.index());
                }
            }
        }, HttpExecution.defaultContext());

        // wrap up the future into a Promise
        F.Promise<Result> resultPromise = F.Promise.wrap(resultFuture);
        
        // timeout and error handling
        return resultPromise.recover(new F.Function<Throwable, Result>() {
            @Override
            public Result apply(Throwable throwable) throws Throwable {
                flash("error", "Could not connect to the Redis server");
                return redirect(controllers.routes.Application.index()); 
            }
        });
    }

    public static Result chatRoomJs(String username) {
        return ok(views.js.chatRoom.render(username));
    }
    
    /**
     * Handle the chat websocket.
     */
    public static WebSocket<JsonNode> chat(final String username) {
        return new WebSocket<JsonNode>() {
            
            // Called when the Websocket Handshake is done.
            public void onReady(WebSocket.In<JsonNode> in, WebSocket.Out<JsonNode> out) {
                ChatRoom.actor.tell(new ChatRoom.Connect(username, in, out), ActorRef.noSender());
                
                in.onClose(new F.Callback0() {
                    @Override
                    public void invoke() throws Throwable {
                        ChatRoom.actor.tell(new ChatRoom.Quit(username), ActorRef.noSender());
                    }
                });
            }
        };
    }
  
}
