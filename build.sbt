import AssemblyKeys._

organization := "com.github.bytecask"

name := "bytecask"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.1"

resolvers ++= Seq(
  "typesafe" at "http://repo.typesafe.com/typesafe/releases/",
  "scala-tools" at "http://scala-tools.org/repo-releases",
  "maven" at "http://repo1.maven.org/maven2",
  "oracle" at "http://download.oracle.com/maven",
  "java-net" at "http://download.java.net/maven/2"
)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % "0.9.30" % "compile",
  "org.xerial.snappy" % "snappy-java" % "1.1.0-M3",
  "org.scalatest" % "scalatest_2.10.0-RC3"  % "1.8-B1"  % "test"
)

scalacOptions ++= Seq(
  "-deprecation",
  "-Xmigration",
  "-Xcheckinit",
  "-Yinline-warnings",
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

