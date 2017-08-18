#REQUIRES -version 2.0
<#
.SYNOPSIS
    Download the latest build of this component from Maven and start the service
.NOTES
    File Name      : start-service.ps1
    Author         : Jan Helge Maurtvedt jan.helge.maurtvedt@altran.com
    Prerequisite   : PowerShell V2 over Vista and upper.
.EXAMPLE
    ./start-service.ps1
.EXAMPLE
    Example 2
#>
#Set mode variable for DEV, TEST or PROD mode
$mode = 'TEST'

#Set environment variable
$env:IAM_MODE=$mode

# Set service name, version and repository
$A='UserAdminWebApp'
$V='2.14-SNAPSHOT'
$url =  'http://mvnrepo.cantara.no/service/local/artifact/maven/content?r=altran-snapshots&g=net.whydah.identity&a='+$A+'&v='+$V+'&p=jar'
$user = 'altran'
$pwd = ConvertTo-SecureString "l1nkSys" -AsPlainText -Force
$jarstartconfig = "-DIAM_CONFIG=useradminwebapp."+ $mode + ".properties"

# Kill process if this service already runs
#TODO: Line below gives error: Stop-Process : Cannot bind parameter 'InputObject'. Cannot convert the "UserAdministration" value of type "System.Strin
#                         g" to type "System.Diagnostics.Process".
# kill -f $A

# Build jarfilename, destination path, etc
$jarfile = $A+'.'+$V+'.jar'
$jardestination = Split-Path -Parent -Path $MyInvocation.MyCommand.Definition
$jardestination+= "\"+$jarfile

#Download the latest jar file from mvnrepo
$creds = New-Object System.Management.Automation.PSCredential ($user, $pwd)
$webclient = new-object System.Net.WebClient
$webclient.Credentials = $creds
$webclient.DownloadFile($url,$jardestination)

#Start the service
#Todo: Fix so it starts in background
java -jar $jarstartconfig $jarfile
