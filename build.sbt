import AssemblyKeys._

organization := "com.github.bytecask"

name := "bytecask"

version := "1.0-SNAPSHOT"

scalaVersion := "2.9.1"

resolvers ++= Seq(
  "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  "scala-tools" at "http://scala-tools.org/repo-releases",
  "maven" at "http://repo1.maven.org/maven2",
  "oracle" at "http://download.oracle.com/maven",
  "java-net" at "http://download.java.net/maven/2"
)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "0.9.30" % "compile",
  "org.xerial.snappy" % "snappy-java" % "1.0.4.1",
  "org.scala-tools.testing" %% "scalacheck" % "1.9" % "test",
  "org.scalatest" %% "scalatest" % "1.6.1"% "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-Xmigration",
  "-Xcheckinit",
  "-optimise",
  "-encoding", "utf8"
)

javacOptions ++= Seq("-source", "1.7")

publishTo <<= (version) { version: String =>
  val nexus = "https://oss.sonatype.org/content/repositories/"
  if (version.trim.endsWith("SNAPSHOT"))
    Some("snapshots" at nexus + "snapshots/")
  else
    Some("releases"  at nexus + "releases/")
}

credentials += Credentials(Path.userHome / ".ivy2" / ".credentials")

seq(assemblySettings: _*)

test in assembly := {}

