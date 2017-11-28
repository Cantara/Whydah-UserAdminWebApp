<!DOCTYPE html>
<html ng-app="UseradminApp">
    <head>
        <meta charset="utf-8"/>
        <meta name="viewport" content="width=device-width, initial-scale=1"/>
        <link rel="icon" type="image/png" href="img/favicon.ico" />
        <title>Whydah Useradmin</title>
        
        <link href="css/bootstrap-glyphicons.css" rel="stylesheet">
        <link href="css/bootstrap-3.3.7.min.css" rel="stylesheet">
        <link href="css/main.css" rel="stylesheet">
        <link href="css/autocomplete.css" rel="stylesheet">
        <link href="css/nprogress.css" rel="stylesheet">
        <link href="css/xeditable.min.css" rel="stylesheet">
    </head>
    <style>
   			
			.navbar-custom {
			  background-color:#0c7bbe;
			  color:#ffffff;
			  border-radius:0;
			}
			
			.navbar-custom .navbar-nav > li > a {
			  color:#fff;
			}
			
			.navbar-custom .navbar-nav > .active > a {
			  color: #ffffff;
			  background-color:transparent;
			}
			
			.navbar-custom .navbar-nav > li > a:hover,
			.navbar-custom .navbar-nav > li > a:focus,
			.navbar-custom .navbar-nav > .active > a:hover,
			.navbar-custom .navbar-nav > .active > a:focus,
			.navbar-custom .navbar-nav > .open >a {
			  text-decoration: none;
			  background-color: #0c7bbe;
			}
			
			.navbar-custom .navbar-brand {
			  color:#eeeeee;
			}
			.navbar-custom .navbar-toggle {
			  background-color:#eeeeee;
			}
			.navbar-custom .icon-bar {
			  background-color:#0c7bbe;
			}
   			
    </style>
    <body ng-controller="MainCtrl">

        <nav class="navbar navbar-default navbar-custom navbar-fixed-top" role="navigation">
            <div class="container-fluid">
                <div class="navbar-header">
                    <button type="button" class="navbar-toggle" data-toggle="collapse" data-target=".navbar-collapse">
                        <span class="sr-only">Toggle navigation</span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                        <span class="icon-bar"></span>
                    </button>
                    <a class="navbar-brand" href="#"><img src="img/whydah.png" alt="Whydah" style="max-height: 90%; display: inline;"/> Whydah Useradmin</a>
                </div>
                <div class="collapse navbar-collapse">
                    <ul class="nav navbar-nav">
                        <li ng-class="{active: session.activeTab == 'user'}"><a href="#/user">Users</a></li>
                        <li ng-class="{active: session.activeTab == 'application'}"><a href="#/application">Applications</a></li>
                        <li ng-class="{active: session.activeTab == 'about'}"><a href="#/about">About Whydah</a></li>
                    </ul>
                    <ul class="nav navbar-nav navbar-right">
                        <li><a id="logout" href="${logOutUrl}">Log out <strong>${realName}</strong></a></li>
                    </ul>
                </div>
            </div>
        </nav>
      

        <!-- Logon timeout Modal -->
        <div class="modal fade" id="timeoutmodal" tabindex="-1" role="dialog" aria-labelledby="timoutlabel" aria-hidden="true" ng-controller="MainCtrl">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <button type="button" class="close" data-dismiss="modal" aria-hidden="true">&times;</button>
                        <h4 class="modal-title" id="timeoutlabel">
                            Login for <strong>${realName}</strong> timed out
                        </h4>
                    </div>

                        <div class="modal-body">
                            You have to log in again
                        </div>
                        <div class="modal-footer">
                            <a href="${logOutUrl}" type="button" class="btn btn-success" data-dismiss="modal">
                                Go to login
                            </a>
                        </div>
                </div>
            </div>
        </div>

        <!-- Message container -->
        <div class="container-fluid">
            <div ng-view id="mainview"></div>
            <div id="messageContainer">
                <div ng-repeat="msg in messages.list" class="alert alert-{{msg.type}}" id="alertMessage">
                    <button type="button" class="close" aria-hidden="true" ng-click="removeMessage($index)">&times;</button>
                    {{msg.text}}
                </div>
            </div>
        </div>

        <script>
            var baseUrl = "${baseUrl}";
            var statUrl = "${statUrl}";
            var userName = "${realName}";
        </script>

        <!-- Framework and tools -->
        <script src="js/lib/jquery/3.2.1/jquery.min.js"></script>
        <script src="js/lib/bootstrap-3.3.7.min.js"></script>
        <!--<script src="js/lib/angular/latest/angular.min.js"></script> !-->
        
        <script src="js/lib/angular/latest/angular.min.js"></script>
        <script src="js/lib/angular/latest/angular-route.min.js"></script>
        <script src="js/lib/angular/latest/angular-animate.min.js"></script>
		

        <!-- Libs -->
        <script src="js/lib/bindHtml.js"></script>
        <script src="js/lib/position.js"></script>
        <script src="js/lib/tooltip.js"></script>
        <script src="js/lib/autocomplete.js"></script>
        <script src="js/lib/smart-table.debug.js"></script>
        <script src="js/lib/smart-table-directives.js"></script>
        <script src="js/lib/elastic.js"></script>
        <script src="js/lib/FileSaver.js"></script>
        <script src="js/lib/ngprogress.js"></script>
        <script src="js/lib/md5.js"></script>
        <script src="js/lib/angularjs-dropdown-multiselect.min.js"></script>
        <script src="js/lib/dirPagination.js"></script>
		<script src="js/lib/xeditable.min.js"></script>

        <!-- Main application -->
        <script src="js/UseradminApp.js"></script>

        <script src="js/MessageService.js"></script>
        <script src="js/UserService.js"></script>
        <script src="js/ApplicationService.js"></script>

        <script src="js/UserCtrl.js"></script>
        <script src="js/UserdetailCtrl.js"></script>
        <script src="js/ApplicationCtrl.js"></script>
        <script src="js/ApplicationdetailCtrl.js"></script>
        <script src="js/RoleCtrl.js"></script>

        <!-- Directives -->
        <script src="js/directives/triStateCheckbox.js"></script>
        <script src="js/directives/editTable.js"></script>
        <script src="js/directives/modal.js"></script>
        <script src="js/directives/imageupload-directives.js"></script>
        <script src="js/directives/demoFileModel.js"></script>
		<script src="js/directives/appEditField.js"></script>
		<script src="js/directives/mytabs.js"></script>

    </body>
</html>