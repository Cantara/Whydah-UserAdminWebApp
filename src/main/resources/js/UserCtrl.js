UseradminApp.controller('UserCtrl', function($scope, $http, $routeParams, Users, Applications, ngProgressFactory, $interval) {

	$scope.session.activeTab = 'user';

	$scope.users = Users;
	$scope.applications = Applications;

	$scope.form = {};

	$scope.orderByColumn = 'username';
	$scope.orderReverse = false;

	$scope.addRoleForMultiple = false;
	
	$scope.importing=false;
	$scope.exporting=false;
	
	$scope.showProgress = false;
	$scope.showExProgress = false;

	var noUsersSelectedMessage = 'Please select a user first!';
	Users.requiredMessage = noUsersSelectedMessage;

	$scope.$watch('users.selected', function(){
		Users.requiredMessage = (Users.selected) ? '' : noUsersSelectedMessage;
	});

	$scope.onSearchBoxChange = function() {
		if(Users.searchQuery===''){
		  console.log("this is called");
		  //Users.search($scope.searchQuery);
		  Users.pagingQuery();
		}
		
	}
	
	$scope.searchUsers = function() {
		
		Users.pagingQuery();
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
		}).then(function(data){
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
	    Users.showMessage('info', "Loading user history. Please wait a moment.");
	    Users.get(id, function(){
	    	  Users.getLog(id, function(){
	    	      $('#userLog').modal('show');
	    	    });
	      });
    }

    $scope.activateUserCrm = function(id,personRef) {
	    console.log('Activating user Crm...', id);
	    Users.showMessage('info', "Loading user Crm. Please wait a moment.");
	    Users.get(id, function(){
	    	  Users.getCrm(id, personRef, function(){
	    	      $('#userCrm').modal('show');
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
		//Users.search();
		Users.pagingQuery();
		
		//progress setup when importing users
		
		$scope.progressbar = ngProgressFactory.createInstance();
		$scope.progressbar.setParent(document.getElementById('progress'));
		
		//for export
		
		$scope.exprogressbar = ngProgressFactory.createInstance();
		$scope.exprogressbar.setParent(document.getElementById('exprogress'));


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
		$scope.exporting=true;
		$scope.exprogressbar.set(0);
		for(var i=0; i<Users.getSelectedUsers().length; i++) {			
			Users.getRolesForThisUser(Users.getSelectedUsers()[i], function(){
				$scope.showExProgress=true;
				
				if($scope.exprogressbar){
					$scope.exprogressbar.set(Users.fullList.length*100/Users.getSelectedUsers().length);
				}
				
				if(Users.fullList.length === Users.getSelectedUsers().length){
					$scope.exprogressbar.set(100);
					$scope.showExProgress=false;
					
					var blob = new Blob([angular.toJson(Users.fullList, true)], {type: "text/plain;charset=utf-8"});
					saveAs(blob, "users-selected-" + pad(Users.currentPage, 5) + ".json");
					
					$scope.exporting=false;
				}
			});
		}
	}

	$scope.exportUsers = function() {
		$scope.exporting=true;
		if($scope.exprogressbar){
			$scope.exprogressbar.set(0);
		}
		
		Users.exportAllUSers(function(data, pageNumber, pageSize, totalItems){
			
			var totalPages = Math.ceil(totalItems/pageSize);
			//some export progress
			$scope.showExProgress=true;
			if($scope.exprogressbar){
				$scope.exprogressbar.set(pageNumber*100/totalPages);
			}
			
			
			var blob = new Blob([angular.toJson(data)], {type: "text/plain;charset=utf-8"});
			saveAs(blob, "users-" + pad(pageNumber, 5) +  ".json");
			
			if(pageNumber==totalPages){
				$scope.exprogressbar.set(100);
				$scope.showExProgress=false;
				$scope.exporting=false;
			}
			
			
		});
	}
	
	function pad(num, size) {
		return ('000000000' + num).substr(-size);
	}

  $scope.userLogProperties = [
    {value: 'userLog', required: false, type: 'json', validationMsg:'The input must be valid json. Recomend http://jsonlint.com for manual validation.'},
  ];

  $scope.userCrmProperties = [
    {value: 'userCrm', required: false, type: 'json', validationMsg:'The input must be valid json. Recomend http://jsonlint.com for manual validation.'},
  ];


	$scope.importUsers = function(){	 
		Users.setDuplicateList(null);
		$('#UserImport').modal('show');
		//Users.search()
		Users.pagingQuery();
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
				$scope.importing = true;
				console.log("Ready to import users now.");
				console.log("Timer has been started, to update import progress.");
				//ask server for the progress each 2 secs
				theInterval = $interval(function(){
					Users.getImportProgress(hex_md5(file.name), function(data){
						if(data>0){
							//can close the modal now
							$('#UserImport').modal('hide');
							$scope.showProgress=true;
							if($scope.progressbar){
								$scope.progressbar.set(data);
							}
							
							if(data==100){
								$scope.showProgress=false;
							}
						} 
					});
	            }, 1000);
			}
			

			promise.then(function (response) {
				
				if(response){
					var pattern = /^error/i;
					var result =  /^error/i.test(response.data.result);
					if(/^error/i.test(response.data.result)===true){
						$scope.showProgress=false;
						$interval.cancel(theInterval);
						$scope.importing = false;
						Users.showMessage('danger','An error has occurred: ' + response.data.result);
						return;
					} else if(/^ok/i.test(response.data.result)===true){
						$scope.showProgress=false;
						$interval.cancel(theInterval);
						Users.showMessage('success', "Imported successfully");
						$('#UserImport').modal('hide');
						$scope.importing = false;
						//refresh
						//Users.search();
						Users.pagingQuery();
						return;
					} else {
			    		  Users.setDuplicateList(response.data);
			    		  return;
			    	}
				}
			}, function (response) {
				Users.showMessage('danger','An error has occurred: ' + response.data.result);
			})
		}

	}
	
	 $scope.pageChangeHandler = function(num) {
		  Users.pagingQuery();
	};

});