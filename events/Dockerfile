FROM hseeberger/scala-sbt:11.0.10_1.5.2_2.13.6 AS build
COPY target/scala-2.13/github-alerts-scala-events-david.corral-assembly-0.0.1-SNAPSHOT.jar /usr/app/github-alerts-scala-events-david.corral.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/usr/app/github-alerts-scala-events-david.corral.jar"]
