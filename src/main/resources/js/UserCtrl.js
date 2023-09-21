UseradminApp.controller('UserCtrl', ['$scope', '$http', '$routeParams', 'Users', 'Applications', '$interval', 'uibDateParser', function($scope, $http, $routeParams, Users, Applications, $interval, uibDateParser) {

	$scope.session.activeTab = 'user';

	$scope.users = Users;
	$scope.applications = Applications;

	$scope.format = 'yyyy/MM/dd';
	$scope.form = {};
	$scope.from_date = addDays(new Date(), -1);
	$scope.to_date = addDays(new Date(), 1);
	

	$scope.orderByColumn = 'username';
	$scope.orderReverse = false;

	$scope.addRoleForMultiple = false;
	$scope.displayActivityList = [];


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
	
	function addDays(date, days) {
		 var result = new Date(date);
		 result.setDate(result.getDate() + days);
		 return result;
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


	function dateToString(date){
		 return date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate();
	}
	
	$scope.activateUserLog = function(userid,username) {
		console.log('Activating usersession log...');
		Users.showMessage('info', "Loading user history. Please wait a moment.");
		
		Users.get(userid, function(){
			Users.getUserSessionLog(userid, dateToString($scope.from_date), dateToString($scope.to_date), function(){
				$('#userLog').modal('show');
			});
		});
	}
	
	$scope.activateUserLogonLog = function(userid,username) {
		console.log('Activating userlogon log...');
		Users.showMessage('info', "Loading user history. Please wait a moment.");
		
		Users.get(userid, function(){
			Users.getUserLogonLog(userid, dateToString($scope.from_date), dateToString($scope.to_date), function(){
				$('#userLogonLog').modal('show');
			});
		});
	}
	
	$scope.queryUserLogonLogs = function(){
		Users.showMessage('info', "Loading usersession history. Please wait a moment.");
		
		Users.getUserLogonLog(Users.user.uid, dateToString($scope.from_date), dateToString($scope.to_date), function(){
			
		});
	}
	
	$scope.queryUserSessionLogs = function(){
		Users.showMessage('info', "Loading userlogon history. Please wait a moment.");
		
		Users.getUserSessionLog(Users.user.uid, dateToString($scope.from_date), dateToString($scope.to_date), function(){
			
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
		if(Users.list.length<1) {
			Users.pagingQuery();
			Applications.searchAll();
		}
		
		//progress setup for uploading
		if(Users.uploadprogressbar){
			Users.uploadprogressbar.setParent(document.getElementById('uploadprogress'));
		}
		

		//progress setup when importing users

		if(Users.progressbar) {			
			Users.progressbar.setParent(document.getElementById('progress'));
		}
		

		//for export
		if(Users.exprogressbar) {
			Users.exprogressbar.setParent(document.getElementById('exprogress'));
		}
		
		// Don't hide application-filter menu when clicking an option
		$('.dropdown-menu').click(function(e) {
			e.stopPropagation();
		});
	}

	init();

	$scope.exportSelectedUsers=function(){
		Users.fullList = []; //clear now
		Users.exporting=true;
		Users.exprogressbar.set(0);
		for(var i=0; i<Users.getSelectedUsers().length; i++) {			
			Users.getRolesForThisUser(Users.getSelectedUsers()[i], function(){
				Users.showExProgress=true;

				if(Users.exprogressbar){
					Users.exprogressbar.set(Users.fullList.length*100/Users.getSelectedUsers().length);
				}

				if(Users.fullList.length === Users.getSelectedUsers().length){
					if(Users.exprogressbar){
						Users.exprogressbar.set(100);
					}
					Users.showExProgress=false;

					var blob = new Blob([angular.toJson(Users.fullList, true)], {type: "text/plain;charset=utf-8"});
                    var today = new Date();
                    var dd = String(today.getDate()).padStart(2, '0');
                    var mm = String(today.getMonth() + 1).padStart(2, '0'); //January is 0!
                    var yyyy = today.getFullYear();

                    today = mm + '-' + dd + '-' + yyyy;
					
					saveAs(blob, "users-selected-" + today+"-"+pad(Users.currentPage, 5) + ".json");
					

					Users.exporting=false;
				}
			});
		}
	}


	$scope.exportUsers = function() {
		Users.exporting=true;
		if(Users.exprogressbar){
			Users.exprogressbar.set(0);
		}
		var zip = new JSZip();

		Users.exportAllUSers(function(data, pageNumber, pageSize, totalItems){

			var totalPages = Math.ceil(totalItems/pageSize);
			//some export progress
			Users.showExProgress=true;
			if(Users.exprogressbar){
				Users.exprogressbar.set(Math.ceil(pageNumber*100/totalPages));
			}


			var blob = new Blob([angular.toJson(data)], {type: "text/plain;charset=utf-8"});
			var today = new Date();
            var dd = String(today.getDate()).padStart(2, '0');
            var mm = String(today.getMonth() + 1).padStart(2, '0'); //January is 0!
            var yyyy = today.getFullYear();

            today = mm + '-' + dd + '-' + yyyy;

			zip.file("users-" + today +"-"+ pad(pageNumber, 5) +  ".json", angular.toJson(data));
			//saveAs(blob, "users-" + today +"-"+ pad(pageNumber, 5) +  ".json");

			if(pageNumber==totalPages){
				if(Users.exprogressbar){
					Users.exprogressbar.set(100);
				}
				Users.showExProgress=false;
				Users.exporting=false;

				zip.generateAsync({
					type: "blob"
				  })
				  .then(function(content) {
					// see FileSaver.js
					saveAs(content, "users-" + today + ".zip");
				  });
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
		if(Users.theInterval){
			$interval.cancel(Users.theInterval);
		}
		if(Users.theUploadInterval){
			$interval.cancel(Users.theUploadInterval);
		}
	});

	
	$scope.uploadFile = function () {



		var file = $scope.myFile;
		if(file && !Users.importing){

			Users.importing = true;
			Users.progressbar.set(0);
			Users.uploadprogressbar.set(0);
			
			
			var promise = Users.importUsers(file);


			
			
			//ask server for the import progress. If there is a status, we just display it. Otherwise, we just hide the progress bar
			Users.theInterval = $interval(function(){
				Users.getImportProgress(hex_md5(file.name), function(data){
					if(data>0){
						
						//can close the modal now
						$('#UserImport').modal('hide');
						Users.showProgress=true;
						if(Users.progressbar){
							Users.progressbar.set(data);
						}

						if(data==100){
							Users.showProgress=false;
						}
					} 
				});
			}, 1000);

			//ask server for the upload progress. If there is a status, we just display it. Otherwise, we just hide the progress bar

			Users.theUploadInterval = $interval(function(){
				Users.getUploadProgress(hex_md5(file.name), function(data){
					
					if(data>0){
					
						Users.showUploadProgress=true;
						if(Users.uploadprogressbar){
							Users.uploadprogressbar.set(data);
						}

						if(data==100){
							Users.showUploadProgress=false;
						}
					} 
				});
			}, 1000);

			promise.then(function (response) {

				if(response){
					var pattern = /^error/i;
					var result =  /^error/i.test(response.data.result);
					if(/^error/i.test(response.data.result)===true){
						closeImportUploadProgress();
						
						Users.showMessage('danger','An error has occurred: ' + response.data.result);
						return;
					} else if(/^ok/i.test(response.data.result)===true){
						closeImportUploadProgress();
						Users.showMessage('success', "Imported successfully");
						$('#UserImport').modal('hide');
			
						//refresh
						Users.pagingQuery();
						return;
					} else {
						
						closeImportUploadProgress();
						Users.setDuplicateList(response.data);
						console.log("Duplicates found " + Users.duplicatelist.length + " items");
						return;
					}
				}
			}, function (response) {
				
				Users.showMessage('danger', 'Operation failed - Status code: ' + response.data.status + " - " +  response.data.message);
			})
		}

	}
	
	function closeImportUploadProgress(){
		//upload is finished now, no need to fetch upload progress
		Users.showUploadProgress=false;
		if(Users.uploadprogressbar){
			Users.uploadprogressbar.set(100);
		}
		$interval.cancel(Users.theUploadInterval);
		//no need to fetch import progress
		Users.showProgress=false;
		if(Users.progressbar){
			Users.progressbar.set(100);
		}
		$interval.cancel(Users.theInterval);
		Users.importing = false;
		$('#myFileField').val('');
	}
	
	$scope.closeImport = function() {
		var file = $scope.myFile;
		
		if(file){

			var promise = Users.removeUploadedFile(file);
			
		}
		closeImportUploadProgress();
		
	
	}

	$scope.pageChangeHandler = function(num) {
		Users.pagingQuery();
	};

}]);