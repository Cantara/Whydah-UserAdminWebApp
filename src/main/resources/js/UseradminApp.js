var UseradminApp = angular.module('UseradminApp', ['ngRoute', 'ngAnimate', 'ui.bootstrap.tooltip', 'autocomplete', 'smart-table', 'ngProgress', 'angularjs-dropdown-multiselect', 'angularUtils.directives.dirPagination', 'imageupload', 'xeditable','angularModalService', 'ui.bootstrap']);

UseradminApp.constant('JSZip', window.JSZip)
.run(function($rootScope) {
    $rootScope.JSZip = window.JSZip;
});


UseradminApp.constant('ConstantValues', {

    
	clientsAutoUpdateLogInterval: 2*60*1000 //clear cache and auto fetch occurs each 2 minutes

});

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

UseradminApp.config(['$locationProvider', function($locationProvider) {
	  $locationProvider.hashPrefix('');
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

UseradminApp.filter('wrap', function () {
  return function (value, wordwise, max, tail) {
    if (!value) return '';

    max = parseInt(max, 10);
    if (!max) return value;
    if (value.length <= max) return value;

    value = value.substr(0, max);
    if (wordwise) {
      var lastspace = value.lastIndexOf(' ');
      if (lastspace != -1) {
        value = value.substr(0, lastspace);
      }
    }

    return value + (tail || ' …');
  };
});


UseradminApp.run(function(editableOptions) {
	  editableOptions.theme = 'bs3'; // bootstrap3 theme. Can be also 'bs2', 'default'
});
