name := "ShibbolethSPBootstrap"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "org.jclouds" % "jclouds-core" % "1.6.0"
)     

play.Project.playJavaSettings
