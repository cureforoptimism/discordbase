FROM amazoncorretto:17

ENV TZ="America/Denver"
ADD . /discordbase
WORKDIR discordbase
RUN ./gradlew bootJar

CMD java -jar /discordbase/build/libs/discordbase-1.0-SNAPSHOT.jar
