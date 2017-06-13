UseradminApp.controller('RoleCtrl', function($scope, Users, Applications) {

  $scope.role = {
    applicationRoleName: '',
    applicationRoleValue: '',
    applicationId: '',
    applicationName: '',
    organizationName: ''
  }

  $scope.applications = Applications;
  var test = [
    {
        applicationName: 'Whydah',
        applicationId: 3,
        organizations: [
            'Developer',
            'Tester',
            'Designer',
            'Sales'
        ]
    },
    {
        applicationName: 'ACS',
        applicationId: 4,
        organizations: [
            'Oslo',
            'Stockholm',
            'Gothenburg'
        ]
    }
  ];

  $scope.currentApplication = {};

  // sets current application
  $scope.setCurrentApplication = function(appId){
    angular.forEach(Applications.list, function(app){
        if(app.id == appId) $scope.currentApplication = app;
    });
  }

  $scope.dict = {
    en: {
      application: 'Application',
      organization: 'Organization',
      applicationRoleName: 'Role',
      applicationRoleValue: 'Value'
    }
  }

  $scope.addRole = function() {
    var addRoleCallBack = function(){
        $scope.form.roleDetail.$setPristine();
    };
    if($scope.addRoleForMultiple) {
        Users.addRoleForSelectedUsers($scope.role, addRoleCallBack);
    } else {
        Users.addRoleForCurrentUser($scope.role, addRoleCallBack);
    }
  }

});