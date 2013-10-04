import play.Project._

name := "websocket-chat"

version := "1.0"

javacOptions += "-Xlint:deprecation"     

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "com.typesafe" %% "play-plugins-redis" % "2.1.1"
)    

resolvers += "org.sedis" at "http://pk11-scratch.googlecode.com/svn/trunk"

playJavaSettings
