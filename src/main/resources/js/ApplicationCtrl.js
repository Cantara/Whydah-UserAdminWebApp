UseradminApp.controller('ApplicationCtrl', function($scope, $http, $routeParams, Users, Applications) {

  $scope.session.activeTab = 'application';

  $scope.users = Users;
  $scope.applications = Applications;
  $scope.form = {};
  $scope.items = ['item1', 'item2', 'item3'];

  $scope.orderByColumn = 'name';
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

  $scope.activateApplicationDetail = function(id) {
    console.log('Activating application detail...', id);
    Applications.get(id, function(){
        //$scope.form.userDetail.$setPristine();
        $('#applicationdetail').modal('show');
    });
  }


  $scope.newApplicationDetail = function() {
    Applications.application = {isNew: true};
    $scope.application = {isNew: true};
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

  // -- Should be in ApplicationdetailCtrl ---
  $scope.applicationProperties = [
    {value: 'id', readOnly: 'true'},
    //{value: 'id',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
    {value: 'name (*)',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
    {value: 'defaultOrganizationName (*)',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
    {value: 'defaultRoleName (*)',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
    {value: 'description',    required: false, type: 'text'},
    {value: 'applicationUrl',     required: false, type: 'url', validationMsg: 'Must be valid URL.'},
    {value: 'logoUrl',    required: false, type: 'url', validationMsg: 'Must be valid URL.'},
    {value: 'secret',     minLength: 12, maxLength: 254, required: false, type: 'text', validationMsg:'Must be between 12-254 characters long. No spaces allowed.'},


  ];
  $scope.dict = {
    en: {
      name: 'Application Name',
      id: 'Application Id',
      defaultOrganizationName: 'Default Organization Name',
      defaultRoleName: 'Default Role Name',
      applicationUrl: 'URL to Application',
      description: 'Description of Application',
      logoUrl: 'URL to Application Logo',
      secret: 'Initial Application Secret',
    }
  }

  $scope.save = function() {
    // Make sure these $scope-values are properly connected to the view
    if($scope.form.applicationDetail.$valid){
      if(Applications.application.isNew) {
        var newApplication = angular.copy(Applications.application);
        delete newApplication.isNew;
        Applications.add(newApplication, function(){
          delete Applications.application.isNew;
          $scope.form.applicationDetail.$setPristine();
        });
      } else {
       Applications.save(Applications.application, function(){
          $scope.form.applicationDetail.$setPristine();
        });
      }
    } else {
      console.log('Tried to save an invalid form.');
    }
  }

});