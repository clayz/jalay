name := "core"

Common.settings

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "mysql" % "mysql-connector-java" % "5.1.18",
  "commons-lang" % "commons-lang" % "2.6",
  "commons-io" % "commons-io" % "2.4",
  "com.github.mumoshu" %% "play2-memcached" % "0.4.0",
  "com.typesafe" %% "play-plugins-mailer" % "2.1-RC2",
  "org.apache.httpcomponents" % "httpcomponents-client" % "4.3.2",
  "org.apache.httpcomponents" % "httpmime" % "4.3.2",
  "com.googlecode.json-simple" % "json-simple" % "1.1",
  "net.tanesha.recaptcha4j" % "recaptcha4j" % "0.0.7",
  "org.twitter4j" % "twitter4j-core" % "4.0.1",
  "org.clapper" %% "argot" % "1.0.1"
)

play.Project.playScalaSettings
