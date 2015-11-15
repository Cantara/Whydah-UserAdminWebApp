UseradminApp.controller('ApplicationCtrl', function($scope, $http, $routeParams, Users, Applications) {

  $scope.session.activeTab = 'application';

  $scope.users = Users;
  $scope.applications = Applications;
  $scope.form = {};
  $scope.items = ['item1', 'item2', 'item3'];

  $scope.orderByColumn = 'id';
  $scope.orderReverse = false;

  $scope.changeOrder = function(orderByColumn) {
    $scope.orderByColumn = orderByColumn;
    $scope.orderReverse = !$scope.orderReverse;
  }

  $scope.searchApps = function() {
  	Applications.search($scope.searchQuery);
  }

  function init() {
    Applications.search();
  }

  if(Applications.list.length<1) {
    init();
  }

  $scope.activateApplicationDetail = function(applicationId) {
    console.log('Activating application detail...', applicationId);
    applicationId = 100;
    Applications.get(applicationId, function(){
        //$scope.form.userDetail.$setPristine();
        $('#applicationdetail').modal('show');
    });
  }
  $scope.newApplicationDetail = function() {
    Applications.application = {isNew: true};
    //Users.userRoles = {};
    //$scope.form.applicationDetail.$setPristine();
    $('#applicationdetail').modal('show');
    /*
    var modalInstance = $uibModal.open({
      animation: $scope.animationsEnabled,
      templateUrl: 'template/applicationdetail.html',
      controller: 'ApplicationdetailCtrl',
      //size: size,
      resolve: {
        application: {isNew: true},
        items: function () {
          return $scope.items;
        }
      }
    });
    */
  }

});