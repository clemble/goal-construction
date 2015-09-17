FROM java:8-jre
MAINTAINER antono@clemble.com

EXPOSE 10005

ADD ./buildoutput/goal-construction.jar /data/goal-construction.jar

CMD java -jar -Dspring.profiles.active=cloud -Dserver.port=10005 -Dlogging.config=classpath:logback.cloud.xml /data/goal-construction.jar
