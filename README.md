UserAdminWebApp
========================

![GitHub tag (latest SemVer)](https://img.shields.io/github/v/tag/Cantara/Whydah-UserAdminWebApp) ![Build Status](https://jenkins.quadim.ai/buildStatus/icon?job=Whydah-UserAdminWebApp) ![GitHub commit activity](https://img.shields.io/github/commit-activity/y/Cantara/Whydah-UserAdminWebApp) [![Project Status: Active – The project has reached a stable, usable state and is being actively developed.](http://www.repostatus.org/badges/latest/active.svg)](http://www.repostatus.org/#active)  [![Known Vulnerabilities](https://snyk.io/test/github/Cantara/Whydah-UserAdminWebApp/badge.svg)](https://snyk.io/test/github/Cantara/Whydah-UserAdminWebApp)


Administration UI for Whydah Users and their mapping to Roles, Applications and Organizations.
Requires UserAdminService, and if authorization is turned on; SSOLoginService and SecurityTokenService.
In order to use the Administration UI the User requires a UserAdmin-role defined in UserIdentityBackend.


![Architectural Overview](https://wiki.cantara.no/download/attachments/37388694/Whydah+infrastructure.png)

Installation
============



* create a user for the service
* run start_service.sh
* ..or create the files from info below:

*update_service.sh*
```
#!/bin/bash

A=UserAdminWebApp
V=SNAPSHOT


if [[ $V == *SNAPSHOT* ]]; then
   echo Note: If the artifact version contains "SNAPSHOT" - the artifact latest greates snapshot is downloaded, Irrelevent of version number!!!
   path="http://mvnrepo.cantara.no/content/repositories/snapshots/net/whydah/identity/$A"
   version=`curl -s "$path/maven-metadata.xml" | grep "<version>" | sed "s/.*<version>\([^<]*\)<\/version>.*/\1/" | tail -n 1`
   echo "Version $version"
   build=`curl -s "$path/$version/maven-metadata.xml" | grep '<value>' | head -1 | sed "s/.*<value>\([^<]*\)<\/value>.*/\1/"`
   JARFILE="$A-$build.jar"
   url="$path/$version/$JARFILE"
else #A specific Release version
   path="http://mvnrepo.cantara.no/content/repositories/releases/net/whydah/identity/$A"
   url=$path/$V/$A-$V.jar
   JARFILE=$A-$V.jar
fi

# Download
echo Downloading $url
wget -O $JARFILE -q -N $url


#Create symlink or replace existing sym link
if [ -h $A.jar ]; then
   unlink $A.jar
fi
ln -s $JARFILE $A.jar
```


*start_service.sh*
```
#!/bin/bash


#  If IAM_MODE not set, use PROD
if [ -z "$IAM_MODE" ]; then
  IAM_MODE=PROD
fi


# If Version is from source, find the artifact
if [ "$Version" = "FROM_SOURCE" ]; then
    # Find the bult artifact
    Version=$(find target/* -name '*.jar' | grep SNAPSHOT | grep -v original | grep -v lib)
else
    Version=UserAdminWebApp.jar
fi

# If IAM_CONFIG not set, use embedded
if [ -z "$IAM_CONFIG" ]; then
  nohup /usr/bin/java -DIAM_MODE=$IAM_MODE  -jar  $Version &
else
  nohup /usr/bin/java -DIAM_MODE=$IAM_MODE  -DIAM_CONFIG=$IAM_CONFIG -jar  $Version &
fi

```


* create useradminwebapp.TEST.properties

```
# standalone=true
standalone=false

#
# Where am I installed and accessible?
#
myuri=http://localhost:9996/useradmin/
# myuri=http://myserver.net/useradmin/


#
#  Uses UserAdminService to get the users
#
useradminservice=http://localhost:9992/useradminservice/

#
# uses SSOLogonservice to redirect non-authenticated users
#
logonservice=http://localhost:9997/sso/
#logonservice=http://sso.myserver.net/sso/

#
# Logs on to SecurityTokenService to participate in the Whydah stack using AppCredentials
#
#tokenservice=http://myserverp.net/tokenservice/
tokenservice=http://localhost:9998/tokenservice/
```

Typical apache setup
====================

```
<VirtualHost *:80>
        ServerName myserver.net
        ServerAlias myserver
        ProxyRequests Off
        <Proxy *>
                Order deny,allow
                Allow from all
        </Proxy>
        ProxyPreserveHost on
                ProxyPass /sso http://localhost:9997/sso
                ProxyPass /uib http://localhost:9995/uib
                ProxyPass /tokenservice http://localhost:9998/tokenservice
                ProxyPass /useradmin http://localhost:9996/useradmin
                ProxyPass /test http://localhost:9990/test/
</VirtualHost>
```




Developer info
==============

* https://wiki.cantara.no/display/iam/Architecture+Overview
* https://wiki.cantara.no/display/iam/Key+Whydah+Data+Structures
* https://wiki.cantara.no/display/iam/Modules

