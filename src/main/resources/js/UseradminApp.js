var UseradminApp = angular.module('UseradminApp', ['ngRoute', 'ngAnimate', 'ui.bootstrap.tooltip', 'autocomplete']);

UseradminApp.config(['$routeProvider',
  function($routeProvider) {
    $routeProvider.
      when('/user/', {
        templateUrl: 'template/usersearch.html',
        controller: 'UserCtrl'
      }).
      when('/application/', {
        templateUrl: 'template/applicationsearch.html',
        controller: 'ApplicationCtrl'
      }).
      when('/about/', {
        templateUrl: 'template/about.html',
        controller: 'AboutCtrl'
      }).
      otherwise({
        redirectTo: '/user/'
      });
}]);

// "Global" variables
UseradminApp.controller('MainCtrl', function($scope, $routeParams, Messages) {
  $scope.conf = {
    lang: 'en',
    errors: []
  }
  $scope.session = {
    activeTab: 'user',
    foundUserList: []
  }
  $scope.messages = Messages;
  $scope.removeMessage = function(index) {
    Messages.remove(index);
  }
  $scope.activateTimeoutModal = function() {
    console.log('Timeout has been invoked...');
    $('#timeoutmodal').modal('show');
  }
});

// TODO: Move to separate js-file
UseradminApp.controller('AboutCtrl', function($scope) {
  $scope.session.activeTab = 'about';
});

// TODO: Move to separate js-file
UseradminApp.controller('AddroleCtrl', function($scope) {
});