UseradminApp.controller('UserCtrl', function($scope, $http, $routeParams, Users, Applications) {

  $scope.session.activeTab = 'user';

  $scope.users = Users;
  $scope.applications = Applications;

  $scope.form = {};

  $scope.orderByColumn = 'username';
  $scope.orderReverse = false;

  $scope.addRoleForMultiple = false;

  var noUsersSelectedMessage = 'Please select a user first!';
  Users.requiredMessage = noUsersSelectedMessage;

  $scope.$watch('users.selected', function(){
    Users.requiredMessage = (Users.selected) ? '' : noUsersSelectedMessage;
  });

  $scope.searchUsers = function() {
    Users.search($scope.searchQuery);
  }

  $scope.clearAllApps = function() {
    console.log('Clear all');
    angular.forEach( Users.applications, function(el, index) {
      el.isSelected = false;
    });
  }

  $scope.getUserByUsername = function(username, callback) {
    console.log('Getting user by username:', username);
    $http({
      method: 'GET',
      url: myHostJsonUsers+username
    }).success(function(data){
      callback(data);
    });
  }

  $scope.activateUserDetail = function(username) {
    console.log('Activating user detail...', username);
    Users.get(username, function(){
      Users.getRolesForCurrentUser( function(){
        $scope.form.userDetail.$setPristine();
        $('#userdetail').modal('show');
      });
    });
  }

  $scope.newUserDetail = function() {
    Users.user = {isNew: true};
    Users.userRoles = {};
    $scope.form.userDetail.$setPristine();
    $('#userdetail').modal('show');
  }

  $scope.addRoleForUsers = function() {
    $scope.addRoleForMultiple = true;
    $('#addrole').modal('show');
  }

  $scope.addRoleForCurrentUser = function() {
    $scope.addRoleForMultiple = false;
    $('#addrole').modal('show');
  }

  $scope.resetPasswordForUsers = function() {
    var selectedUsernames = Users.getSelectedUsernames();
    var selectedUsers = Users.getSelectedUsers();
    if(window.confirm('Are you sure you want to reset password for users: '+selectedUsernames.join(', ')+'?')) {
        console.log('Resetting passwords.');
        for(var i=0; i<selectedUsers.length; i++) {
            Users.resetPassword(selectedUsers[i]);
        }
    }
  }

  $scope.changeOrder = function(orderByColumn) {
    $scope.orderByColumn = orderByColumn;
    $scope.orderReverse = !$scope.orderReverse;
  }
  
  function init() {
    Users.search();
    Applications.search();
    // Don't hide application-filter menu when clicking an option
    $('.dropdown-menu').click(function(e) {
      e.stopPropagation();
    });
  }

  if(Users.list.length<1) {
    init();
  }
  
  $scope.exportSelectedUsers=function(){
	  Users.fullList = []; //clear now
      for(var i=0; i<Users.getSelectedUsers().length; i++) {
		  Users.getRolesForThisUser(Users.getSelectedUsers()[i], function(){
			  if(Users.fullList.length === Users.getSelectedUsers().length){
				  var blob = new Blob([angular.toJson(Users.fullList, true)], {type: "text/plain;charset=utf-8"});
				  saveAs(blob, "users.json");
			  }
		  });
      }
	 
//	  Users.fullList = []; //clear now
//      for(var i=0; i<Users.list.length; i++) {
//		  Users.getRolesForThisUser(Users.list[i], function(){
//			  var blob = new Blob([angular.toJson(Users.fullList, true)], {type: "text/plain;charset=utf-8"});
//			  saveAs(blob, "users.json");
//		  });
//      }
  }
  
  $scope.exportUsers = function() {
	  Users.fullList = []; //clear now
      for(var i=0; i<Users.list.length; i++) {
		  Users.getRolesForThisUser(Users.list[i], function(){
			  if(Users.fullList.length === Users.list.length){
				  var blob = new Blob([angular.toJson(Users.fullList, true)], {type: "text/plain;charset=utf-8"});
			  	  saveAs(blob, "users.json");
			  }
		  });
      }
  }
  
  $scope.importUsers = function(){	 
	  $('#UserImport').modal('show');
  }
  
  $scope.uploadFile = function () {
      var file = $scope.myFile;
      if(file){
	      var uploadUrl = baseUrl + "importUsers", //Url of web service
	      promise = Users.importUsers(file, uploadUrl);
	      promise.then(function (response) {
	    	  if(response){
		    	  var pattern = /^error/i;
		          var result =  /^error/i.test(response.result);
		    	  if(/^error/i.test(response.result)===true){
		    		  Users.showMessage('danger','An error has occurred: ' + response.result);
		    		  return;
		    	  } else if(/^ok/i.test(response.result)===true){
		    		  Users.showMessage('success', "Imported successfully");
		    		  $('#UserImport').modal('hide');
		    		  return;
		    	  }
	    	  }
	      }, function (response) {
	    	  Users.showMessage('danger','An error has occurred: ' + response.result);
	      })
	   }
      
  }

});