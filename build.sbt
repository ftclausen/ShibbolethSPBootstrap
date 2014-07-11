name := "ShibbolethSPBootstrap"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "org.apache.jclouds" % "jclouds-all" % "1.7.3",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.apache.jclouds.api" % "chef" % "1.7.3"
)     

play.Project.playJavaSettings
