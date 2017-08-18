#!/bin/sh
mvn clean package
IAM_MODE=DEV java -jar target/UserAdministration-0.2-SNAPSHOT.jar

