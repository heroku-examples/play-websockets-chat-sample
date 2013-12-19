import play.Project._

name := "websocket-chat"

version := "1.0"

javacOptions += "-Xlint:deprecation"     

libraryDependencies ++= Seq(
  cache,
  javaCore,
  "com.typesafe" %% "play-plugins-redis" % "2.1.1",
  "org.webjars" %% "webjars-play" % "2.2.0",
  "org.webjars" % "bootstrap" % "3.0.3"
)    

resolvers += "org.sedis" at "http://pk11-scratch.googlecode.com/svn/trunk"

playJavaSettings