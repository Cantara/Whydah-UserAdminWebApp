UseradminApp.controller('UserCtrl', function($scope, $http, $routeParams, Users, Applications, ngProgressFactory, $interval) {

	$scope.session.activeTab = 'user';

	$scope.users = Users;
	$scope.applications = Applications;

	$scope.form = {};

	$scope.orderByColumn = 'username';
	$scope.orderReverse = false;

	$scope.addRoleForMultiple = false;
	
	$scope.importing=false;
	$scope.showProgress = false;

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

    $scope.activateUserLog = function(id) {
	    console.log('Activating user log...', id);
	    Applications.showMessage('info', "Loading user history. Please wait a moment.");
	    Users.get(id, function(){

	    	  Users.getLog(id, function(){

	    	      $('#applicationLog').modal('show');

	    	    });

	      });


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
		//progress setup when importing users
		
		$scope.progressbar = ngProgressFactory.createInstance();
		$scope.progressbar.setParent(document.getElementById('progress'));

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
		Users.setDuplicateList(null);
		$('#UserImport').modal('show');
		Users.search()
	}

	 $scope.$on('$destroy', function () {
		 if(theInterval){
			 $interval.cancel(theInterval);
		 }
      });

	var theInterval;
	
	$scope.uploadFile = function () {
		
		
		
		var file = $scope.myFile;
		if(file){
			
			var uploadUrl = baseUrl + "importUsers", //Url of web service
			promise = Users.importUsers(file, uploadUrl);
			
			if(Users.duplicatelist&&Users.duplicatelist.length>0){
				$scope.$parent.importing = true;
				console.log("Ready to import users now.");
				console.log("Timer has been started, to update import progress.");
				//ask server for the progress each 2 secs
				theInterval = $interval(function(){
					Users.getImportProgress(hex_md5(file.name), function(data){
						if(data>0){
							//can close the modal now
							$('#UserImport').modal('hide');
							$scope.$parent.showProgress=true;
							if($scope.$parent.progressbar){
								$scope.$parent.progressbar.set(data);
							}
							
							if(data==100){
								$scope.$parent.showProgress=false;
							}
						} 
					});
	            }, 1000);
			}
			

			promise.then(function (response) {
				if(response){
					var pattern = /^error/i;
					var result =  /^error/i.test(response.result);
					if(/^error/i.test(response.result)===true){
						$scope.$parent.showProgress=false;
						$interval.cancel(theInterval);
						$scope.$parent.importing = false;
						Users.showMessage('danger','An error has occurred: ' + response.result);
						return;
					} else if(/^ok/i.test(response.result)===true){
						$scope.$parent.showProgress=false;
						$interval.cancel(theInterval);
						Users.showMessage('success', "Imported successfully");
						$('#UserImport').modal('hide');
						$scope.$parent.importing = false;
						//refresh
						Users.search();
						return;
					} else {
			    		  Users.setDuplicateList(response.result);
			    		  return;
			    	  }
				}
			}, function (response) {
				Users.showMessage('danger','An error has occurred: ' + response.result);
			})
		}

	}

});