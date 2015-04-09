FROM java:8-jre
MAINTAINER antono@clemble.com

EXPOSE 10005

ADD target/goal-construction-0.17.0-SNAPSHOT.jar /data/goal-construction.jar

CMD java -jar -Dspring.profiles.active=cloud -Dserver.port=10005 /data/goal-construction.jar
