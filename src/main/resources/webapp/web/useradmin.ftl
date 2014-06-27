<!DOCTYPE html>
<html ng-app="UseradminApp">
    <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1"/>
        <link rel="icon" type="image/png" href="" />
        <title>Whydah Useradmin</title>
        <!--<link href="//netdna.bootstrapcdn.com/bootstrap/3.1.1/css/bootstrap.min.css" rel="stylesheet">-->
        <link href="//netdna.bootstrapcdn.com/bootswatch/3.1.1/yeti/bootstrap.min.css" rel="stylesheet">
        <link href="css/main.css" rel="stylesheet">
    </head>
    <body ng-controller="MainCtrl">
        <div class="navbar navbar-inverse navbar-fixed-top" role="navigation">
            <div class="container-fluid">
                <div class="navbar-header">
                    <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                        <span class="sr-only">Toggle navigation</span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                    </button>
                    <a class="navbar-brand" href="#"><img src="img/whydah.png" alt="Whydah" style="max-height: 90%"/> Whydah Useradmin</a>
                </div>
                <div class="collapse navbar-collapse">
                    <ul class="nav navbar-nav">
                        <li ng-class="{active: session.activeTab == 'user'}"><a href="#">Users</a></li>
                        <li ng-class="{active: session.activeTab == 'application'}"><a href="#/application">Applications</a></li>
                        <li ng-class="{active: session.activeTab == 'about'}"><a href="#/about">About Whydah</a></li>
                    </ul>
                    <ul class="nav navbar-nav navbar-right">
                        <li><a href="${logOutUrl}">Log out <strong>${realName}</strong></a></li>
                    </ul>
                </div><!--/.nav-collapse -->
            </div>
        </div>

        <div class="container-fluid">
            <div ng-view id="mainview"></div>
            <div id="messageContainer">
                <div ng-repeat="msg in messages.list" class="alert alert-{{msg.type}}">
                    <button type="button" class="close" aria-hidden="true" ng-click="removeMessage($index)">&times;</button>
                    {{msg.text}}
                </div>
            </div>
        </div>

        <script>
            var baseUrl = "${baseUrl}";
            var userName = "${realName}";
        </script>

        <!-- Framework and tools -->
        <script src="//ajax.googleapis.com/ajax/libs/jquery/1.11.0/jquery.min.js"></script>
        <script src="//netdna.bootstrapcdn.com/bootstrap/3.1.1/js/bootstrap.min.js"></script>
        <script src="//ajax.googleapis.com/ajax/libs/angularjs/1.2.14/angular.min.js"></script>
        <script src="//ajax.googleapis.com/ajax/libs/angularjs/1.2.14/angular-route.min.js"></script>
        <script src="//ajax.googleapis.com/ajax/libs/angularjs/1.2.14/angular-animate.min.js"></script>

        <!-- Libs -->
        <script src="js/lib/bindHtml.js"></script>
        <script src="js/lib/position.js"></script>
        <script src="js/lib/tooltip.js"></script>
        <script src="js/auth.js"></script>

        <!-- Main application -->
        <script src="js/UseradminApp.js"></script>

        <script src="js/MessageService.js"></script>
        <script src="js/UserService.js"></script>
        <script src="js/ApplicationService.js"></script>

        <script src="js/UserCtrl.js"></script>
        <script src="js/UserdetailCtrl.js"></script>
        <script src="js/ApplicationCtrl.js"></script>
        <script src="js/RoleCtrl.js"></script>

        <!-- Directives -->
        <script src="js/triStateCheckbox.js"></script>
        <script src="js/editTable.js"></script>
        <script src="js/modal.js"></script>

    </body>
</html>