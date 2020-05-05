name := "EventCounter"

scalaVersion := "2.12.11"

organization := "com.cafecito"

version := "0.2.0-SNAPSHOT"

resolvers += Resolver.githubPackages("hernansaab")

githubOwner := "cafecito"

githubRepository := "sbt-github-packages"

resolvers += ("Artima Maven Repository" at "http://repo.artima.com/releases").withAllowInsecureProtocol(true)


libraryDependencies += "org.scalactic" %% "scalactic" % "3.1.1"

libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.1" % Test

libraryDependencies += "org.scalamock" %% "scalamock" % "4.4.0" % Test


// https://mvnrepository.com/artifact/org.apache.spark/spark-core
// for memory estimation
libraryDependencies += "org.apache.spark" %% "spark-core" % "2.4.5" % Test


// disable publishing the main jar produced by `package`
Compile / packageBin / publishArtifact := true

// disable publishing the main API jar
Compile / packageDoc / publishArtifact := true

// disable publishing the main sources jar
Compile / packageSrc / publishArtifact := true