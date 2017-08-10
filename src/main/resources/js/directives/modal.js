UseradminApp.directive('uaModal', function() {
  return {
    restrict: 'A',
    scope: { uaModalName: '=', uaModalController: '=' },
    templateUrl: 'template/directives/modal.html',
    controller: function($scope) {
    	console.log('Modal', $scope.uaModalController);
    }
  };
});