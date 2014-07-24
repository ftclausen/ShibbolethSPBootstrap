name := "ShibbolethSPBootstrap"

version := "1.0"

libraryDependencies ++= Seq(
  javaJdbc,
  javaEbean,
  cache,
  "org.apache.jclouds" % "jclouds-all" % "1.7.3",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.apache.jclouds.api" % "chef" % "1.7.3",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.4.1.1",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.4.1.1",
  "com.fasterxml.jackson.core" % "jackson-annotations" % "2.4.1"
)     

play.Project.playJavaSettings
