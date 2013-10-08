# Scalable chat application with Play and WebSockets

This is a version of the [Play](http://www.playframework.com/) websockets [chat sample](https://github.com/playframework/playframework/tree/master/samples/java/websocket-chat) that uses redis pub/sub to allow for a single chatroom that works across multiple nodes.

# Running

The application requires a Redis connection. This is configured in applicaiton.conf:

```
# Redis configuration
redis.uri=${REDISCLOUD_URL}
```

Set the URL into an environment variable or delete this line to run Redis locally on the default port.

Once setting up Redis you can run with

``` bash
$ play run
```

# Running on Heroku

``` bash
$ heroku create --buildpack https://github.com/jamesward/heroku-buildpack-scala.git
$ heroku labs:enable websockets
$ heroku addons:add rediscloud
$ git push heroku master
$ heroku open
```
