UseradminApp.controller('ApplicationCtrl', function($scope, $http, $window, $routeParams, Users, Applications) {

  $scope.session.activeTab = 'application';

  $scope.users = Users;
  $scope.applications = Applications;
  $scope.displayCollectionList = [];
  
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

  if(typeof(Applications.list) != 'undefined' && Applications.list.length<1) {
    init();
  }

  $scope.activateApplicationDetail = function(id) {
    console.log('Activating application detail...', id);
    Applications.get(id, function(){
        //$scope.form.userDetail.$setPristine();
        $('#applicationdetail').modal('show');
    });
  }

  $scope.activateApplicationJson = function(id) {
    console.log('Activating application json...', id);
    Applications.get(id, function(){
      //$scope.form.userDetail.$setPristine();
      $('#applicationJson').modal('show');
      $scope.prettifyJson();
    });
  }

  $scope.exportSelectedApps=function(){
	  var blob = new Blob([JSON.stringify(Applications.getSelectedList(), null, 4)], {type: "text/plain;charset=utf-8"});
	  saveAs(blob, "applications.js");
  }
  
  $scope.exportApps = function() {
	  Applications.search(); //get the latest version
	  var blob = new Blob([JSON.stringify(Applications.list, null, 4)], {type: "text/plain;charset=utf-8"});
	  saveAs(blob, "applications.js");
  }
  
  $scope.importApps = function(){
	  Applications.setDuplicateList(null);
	  $('#applicationdImport').modal('show');
  }
  
  $scope.uploadFile = function () {
      var file = $scope.myFile;
      if(file){
	      var uploadUrl = baseUrl + "import", //Url of web service
	      promise = Applications.importApps(file, uploadUrl);
	      promise.then(function (response) {
	    	  console.log(response);
	    	  var pattern = /^error/i;
	          var result =  /^error/i.test(response.result);
	    	  if(/^error/i.test(response.result)===true){
	    		  Applications.showMessage('danger','An error has occurred: ' + response.result);
	    	  } else if(/^ok/i.test(response.result)===true){
	    		  Applications.showMessage('success', "Imported successfully");
	    		  $('#applicationdImport').modal('hide');
	    	  } else {
	    		  //display a list
	    		  var obj = JSON.parse(response.result);
	    		  Applications.setDuplicateList(response.result);
	    	  }
	      }, function (response) {
	    	  Applications.showMessage('danger','An error has occurred: ' + response);
	      })
	   }
      
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
    {value: 'name',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
    {value: 'defaultOrganizationName',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
    {value: 'defaultRoleName',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
    {value: 'description',    required: false, type: 'text'},
    {value: 'applicationUrl',     required: false, type: 'url', validationMsg: 'Must be valid URL.'},
    {value: 'logoUrl', required: false, type: 'url', validationMsg: 'Must be valid URL.'},
    {value: 'fullTokenApplication',    required: false, type: 'text'},
    {value: 'secret', minLength: 12, maxLength: 254, required: false, type: 'text', validationMsg:'Must be between 12-254 characters long. No spaces allowed.'},
    {value: 'roleNames', required: false, type: 'text', validationMsg:'Comma separated list of available role names'},
    {value: 'orgNames', required: false, type: 'text', validationMsg:'Comma separated list of available organization names'}
  ];

  $scope.applicationJsonProperties = [
    {value: 'applicationJson', required: false, type: 'json', validationMsg:'The input must be valid json. Recomend http://jsonlint.com for manual validation.'},
  ];

  $scope.dict = {
    en: {
      name: 'Application Name (*)',
      id: 'Application Id',
      defaultOrganizationName: 'Default Organization Name (*)',
      defaultRoleName: 'Default Role Name (*)',
      applicationUrl: 'URL to Application',
      description: 'Description of Application',
      logoUrl: 'URL to Application Logo',
      fullTokenApplication: 'Whydah Admin application',
      secret: 'Application Secret',
      applicationJson: 'Json override',
      roleNames: 'Available role names',
      orgNames: 'Available organization names'
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

  $scope.saveFromJson = function() {
    // Make sure these $scope-values are properly connected to the view
    if($scope.form.applicationJson.$valid){
      if(Applications.application.isNew) {
        var newApplication = angular.copy(Applications.application);
        delete newApplication.isNew;
        Applications.addFromJson(newApplication, function(){
          delete Applications.application.isNew;
          $scope.form.applicationJson.$setPristine();
        });
      } else {
        Applications.saveFromJson(Applications.application, function(){
          $scope.form.applicationJson.$setPristine();
        });
      }
    } else {
      console.log('Tried to save an invalid form.');
    }
  }

  $scope.prettifyJson = function() {
    try {
      var jsonObject = JSON.parse($scope.applications.application.applicationJson);
      var prettifiedJson = JSON.stringify(jsonObject, undefined, 2);
      $scope.applications.application.applicationJson = prettifiedJson;
    }catch (e) {
      Messages.add('danger', 'Could not prettify applicationJson. Error while parsing');
    }
  }

  $scope.delete = function() {
    var deleteUser = $window.confirm('Are you absolutely sure you want to delete '+ Applications.application.name +'?');

    if (deleteUser) {
      Applications.delete(Applications.application, function(){
        $scope.form.applicationDetail.$setPristine();
      });
    }
  }

});