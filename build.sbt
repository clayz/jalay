name := "jalay"

Common.settings

play.Project.playScalaSettings

lazy val main = project.in(file(".")).dependsOn(core, api, batch, admin).aggregate(admin).settings(
  scalacOptions ++= Seq("-feature", "-language:reflectiveCalls"),
  sources in(Compile, doc) := Seq.empty
)

lazy val batch = project.in(file("modules/batch")).dependsOn(core, api).settings(
  scalacOptions ++= Seq("-feature"),
  sources in(Compile, doc) := Seq.empty
)

lazy val admin = project.in(file("modules/admin")).dependsOn(core, api).settings(
  scalacOptions ++= Seq("-feature", "-language:implicitConversions"),
  sources in(Compile, doc) := Seq.empty
)

lazy val api = project.in(file("modules/api")).dependsOn(core).settings(
  scalacOptions ++= Seq("-feature", "-language:implicitConversions"),
  sources in(Compile, doc) := Seq.empty
)

lazy val core = project.in(file("modules/core")).settings(
  scalacOptions ++= Seq("-feature", "-language:reflectiveCalls", "-language:postfixOps"),
  sources in(Compile, doc) := Seq.empty,
  // Memcached plugin
  resolvers += "Sonatype OSS Snapshots Repository" at "http://oss.sonatype.org/content/groups/public",
  resolvers += "Spy Repository" at "http://files.couchbase.com/maven2",
  resolvers += "The Seasar Foundation Maven2 Repository" at "http://maven.seasar.org/maven2/"
)
