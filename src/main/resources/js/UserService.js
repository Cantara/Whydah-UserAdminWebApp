UseradminApp.service('Users', function($http, Messages, $q, ngProgressFactory){
	
	this.list = []; //users to display
	this.rows = ""; //how many rows there by list.length
	this.user = {}; //current user object
	this.userRoles = {}; //related roles
	this.searchQuery = ''; //some query
	this.selected = false; //if one particular user is selected
	this.applications = []; //application list
	this.applicationFilter = []; //????
	this.fullList=[]; //it is actually a copy of this.list including full roles for each user
	this.currentPage = 1; //current page number shown
	this.pageSize = 0; //this is volatile as it is configured from the server side (each page should display a maximum number of items)
	this.totalItems = 0; //this is volatile as it will get the total hits from the server side

	//UI for import/export progress
	this.importing=false; //being imported
	this.exporting=false;//being exported
	this.showProgress = false; //should show progress bar for import?
	this.showExProgress = false; //should show progress bar for export?
	this.showUploadProgress = false; //should show progress bar for upload?
	this.uploadprogressbar = ngProgressFactory.createInstance();; //the progress bar for upload
	this.progressbar = ngProgressFactory.createInstance();; //the progress bar for import
	this.exprogressbar= ngProgressFactory.createInstance();; //the progress bar for export
	this.theInterval; //the interval to check import progress on server
	this.theUploadInterval; //the interval to check upload progress on server


	
	
	this.getSelectedUsers = function() {
	    var selectedUsers = [];
	    for(var i=0; i<this.list.length; i++) {
	        if(this.list[i].isSelected)selectedUsers.push(this.list[i]);
	    }
	    return selectedUsers;
	}

	this.getSelectedUsernames = function() {
	    var selectedUsernames = [];
	    var selectedUsers = this.getSelectedUsers();
	    for(var i=0; i<selectedUsers.length; i++) {
	        selectedUsernames.push(selectedUsers[i].username);
	    }
	    return selectedUsernames;
	}

//	this.search = function() {
//		console.log('Searching for users...');
//		var query =  this.searchQuery || '*';
//		var that = this;
//		$http({
//			method: 'GET',
//			url: baseUrl+'users/find/'+query
//			//url: 'json/users.json',
//		}).then(function (response) {		
//			that.rows = response.data.rows;
//			//that.list = JSON.parse(angular.toJson(response.data.result));
//			that.list = response.data.result;
//			
//		}, function(response){
//			// This is most likely due to usertoken timeout - TODO: Redirect to login webapp   
//			console.log('Unable to search', response.data);
//			var status = response.status;
//			switch (status) {
//				case 403: /* Forbidden */
//					Messages.add('danger', 'Unable to seach! Forbidden...');
//					break;
//				case 404:  /* 404 No access */
//					Messages.add('danger', 'Unable to search! No access...');
//					break;
//				case 409:  /* 409 Conflict - will prbably not occur here */
//					Messages.add('danger', 'Search already exists...');
//					break;
//				default:
//			    	Messages.add('danger', 'Search failed with error code: ' + status);
//			}
//
//		});
//		return this;
//	};
	
	
	this.pagingQuery = function() {
		
		var query =  this.searchQuery || '*';
		var that = this;
		console.log('Searching for users on page ' + this.currentPage + ' with query value = ' + query );
		$http({
			method: 'GET',
			url: baseUrl+'users/query/' + that.currentPage + "/" + query
			//url: 'json/users.json',
		}).then(function (response) {		
			
			
			that.rows = response.data.rows;
			that.pageSize = response.data.pageSize;
			that.totalItems = response.data.totalItems;
			that.currentPage = response.data.currentPage;
			that.list = response.data.result;
			
		}, function(response){
			// This is most likely due to usertoken timeout - TODO: Redirect to login webapp   
			console.log('Unable to search', response);
			var status = response.status;
			switch (status) {
				case 403: /* Forbidden */
					Messages.add('danger', 'Unable to seach! Forbidden...');
					break;
				case 404:  /* 404 No access */
					Messages.add('danger', 'Unable to search! No access...');
					break;
				case 409:  /* 409 Conflict - will prbably not occur here */
					Messages.add('danger', 'Search already exists...');
					break;
				default:
			    	Messages.add('danger', 'Search failed with error code: ' + status);
			}

		});
		return this;
	};
	
	this.getRolesForCurrentUser = function(callback) {
        var uid = this.user.uid;
	    console.log('Getting roles for user with uid=', uid);
	    var that = this;
		$http({
			method: 'GET',
			url: baseUrl+'user/'+uid+'/roles/'
		}).then(function (response) {
		    console.log('Got userroles', response.data);
		    that.userRoles = response.data;
		    if(callback) {
		        callback(response.data);
		    }
		});
		return this;
    };
    
	this.getRolesForThisUser=function(u, callback){
		var that = this;				
		$http({
				method: 'GET',
				url: baseUrl+'user/'+u.uid+'/roles/'
			}).then(function (response) {
			    u.roles = response.data;
			    that.fullList.push(u);
			    callback();
			    
			});
	    
		
	}

	this.get = function(uid, callback) {
	    console.log('Getting user with uid=', uid);
	    var that = this;
		$http({
			method: 'GET',
			url: baseUrl+'user/'+uid+'/'
		}).then(function (response) {
			var data = response.data;
		    console.log('Got user', data);
		    that.user = data;
		    if(callback) {
		        callback(data);
		    }
		});
		return this;
	};

    this.getLog = function(id, callback) {
        console.log('Getting User log for uid=', id);
        var that = this;
        $http({
            method: 'GET',
            url: baseUrl+'userlog/'+id+'/'
        }).then(function (response) {
            console.log('Got user log', response.data);
            that.user.userLog = JSON.stringify(response.data, null, 2);
            if(callback) {
                 callback(that.user);
            }
        });
        return this;
    };

    this.getCrm = function(id, personRef, callback) {
        console.log('Getting User Crm for personRef=', personRef);
        var that = this;
        $http({
            method: 'GET',
            url: baseUrl+'usercrm/'+personRef+'/'
        }).then(function (response) {
        	var data = response.data;
            console.log('Got user crm', data);
            that.user.userCrm = JSON.stringify(data, null, 2);
            if(callback) {
                 callback(that.user);
            }
        });
        return this;
    };

    // Current json-request for save
    // jsond: {"personRef":"1", "username":"leon", "firstName":"Leon", "lastName":"Ho", "email":"leon.ho@altran.com", "cellPhone":"993 97 835"}
	this.save = function(user, successCallback) {
	    console.log('Saving user', user);
	    var that = this;
		$http({
			method: 'PUT',
			url: baseUrl+'user/'+user.uid+'/',
			data: user
		}).then(function (response) {
			var data = response.data;
			Messages.add('success', 'User "'+user.username+'" was saved successfully.');
		    //that.search(that.searchQuery);
			that.pagingQuery();
		    if(successCallback){
		        successCallback();
		    }
		}, function (response) {
			var data = response.data;
			var status = response.status;
			Messages.add('danger', 'Oops, something went wrong. User "'+user.username+'" was not saved successfully.');
		});
		return this;
	};

	this.add = function(user, successCallback) {
	    console.log('Adding user', user);
	    var that = this;
		$http({
			method: 'POST',
			url: baseUrl+'user/',
			data: user
		}).then(function (response) {
			var data = response.data;
			Messages.add('success', 'User "'+user.username+'" was added successfully.');
			that.user.uid = data.uid;
			//that.search(that.searchQuery);
			that.pagingQuery();
		    if(successCallback){
		        successCallback();
		    }
		}, function (response) {
			var data = response.data;
			var status = response.status;
			console.log('User was not added', data);
			switch (status) {
				case 403:
					Messages.add('danger', 'User was not added! No access...');
					break;
				case 404:  /* 404 No access */
					Messages.add('danger', 'User was not added! No access...');
					break;
				case 409:  /* 409 Conflict - user exists or was double posted */
					Messages.add('danger', 'User was not added! Already exists...');
					break;
				default:
			    	Messages.add('danger', 'User was not added and! Try again later...');
			}
			$scope.activateTimeoutModal();
		});
		return this;
	};

	this.delete = function(user) {
	    console.log('Deleting user', user);
        var uid = this.user.uid;
	    var that = this;
		$http({
			method: 'DELETE',
			url: baseUrl+'user/'+uid+'/'
		}).then(function (response) {
			var data = response.data;
			var status = response.status;
			Messages.add('success', 'User "'+user.username+'" was deleted successfully.');
			//that.search(that.searchQuery);
			that.pagingQuery();
		});
		return this;
	};

	// ROLES

    this.getRolesForCurrentUser = function(callback) {
        var uid = this.user.uid;
	    console.log('Getting roles for user with uid=', uid);
	    var that = this;
		$http({
			method: 'GET',
			url: baseUrl+'user/'+uid+'/roles/'
		}).then(function (response) {
			var data = response.data;
			var status = response.status;
		    console.log('Got userroles', data);
		    that.userRoles = data;
		    if(callback) {
		        callback(data);
		    }
		});
		return this;
    };

    this.addRoleForUser = function(role, user, successCallback) {
	    console.log('Adding role for user', user, role);
	    var that = this;
		$http({
			method: 'POST',
			url: baseUrl+'user/'+user.uid+'/role/',
			data: role
		}).then(function (response) {
			var data = response.data;
			var status = response.status;
			Messages.add('success', 'Role for user "'+user.username+'" was added successfully.');
			that.getRolesForCurrentUser();
			//that.search(that.searchQuery);
			that.pagingQuery();
			if(successCallback){
			    successCallback();
			}
		});
		return this;
    };

	this.addRoleForCurrentUser = function(role, successCallback) {
	    this.addRoleForUser(role, this.user, successCallback);
	};

	this.addRoleForSelectedUsers = function(role, successCallback) {
	    var selectedUsers = this.getSelectedUsers();
	    for(var i=0; i<selectedUsers.length; i++) {
	        this.addRoleForUser(role, selectedUsers[i], successCallback);
	    }
	};

    this.deleteRoleForUser = function(role, user) {
	    console.log('Deleting role for user', user, role);
	    var that = this;
	    var roleName = role.applicationRoleName;
		$http({
			method: 'DELETE',
			url: baseUrl+'user/'+user.uid+'/role/'+role.roleId,
			data: role
		}).then(function (response) {
			var data = response.data;
			var status = response.status;
			Messages.add('success', 'Role "'+roleName+'" for user "'+user.username+'" was deleted successfully.');
			that.getRolesForCurrentUser();
			//that.search(that.searchQuery);
			that.pagingQuery();
		}, function (response) {
			var data = response.data;
			var status = response.status;
            Messages.add('warning', 'Role "'+roleName+'" for user "'+that.user.username+'" was not deleted.');
            that.getRolesForCurrentUser();
        });
		return this;
    };

	this.deleteRoleForCurrentUser = function(role) {
	    this.deleteRoleForUser(role, this.user);
	};

    this.saveRoleForUser = function(role, user) {
	    console.log('saveRoleForUser ', user, role);
	    var that = this;
	    var roleName = role.applicationRoleName;
		$http({
			method: 'PUT',
			url: baseUrl+'user/'+user.uid+'/role/'+role.roleId,
			data: role
		}).then(function (response) {
			var data = response.data;
			var status = response.status;
			Messages.add('success', 'Role "'+roleName+'" for user "'+that.user.username+'" was saved successfully.');
			that.getRolesForCurrentUser();
			//that.search(that.searchQuery);
			that.pagingQuery();
		}, function (response) {
			var data = response.data;
			var status = response.status;
			Messages.add('warning', 'Role "'+roleName+'" for user "'+that.user.username+'" was not saved.');
			that.getRolesForCurrentUser();
		});
		return this;
    };

    this.saveRoleForCurrentUser = function(role) {
        this.saveRoleForUser(role, this.user);
    };

		this.getUserByUserName = function(username, callback) {
			var query = "username=" + username;
			$http({
				method: 'GET',
				url: baseUrl+'users/find/' + query,
			}).then(function (response) {
				var data = response.data;
				var status = response.status;
				callback(data);
			});
			return this;
		};


    // PASSWORD
   //  UAS  /auth/password/reset/username/
    this.resetPassword = function(user) {
		$http({
			method: 'POST',
//			url: baseUrl+'user/'+user.uid+'/resetpassword'
			url: baseUrl+'auth/password/reset/username/'+user.username
	}).then(function (response) {
			var data = response.data;
			var status = response.status;
			Messages.add('success', 'Reset password mail sent to user "'+user.username+'".');
		}, function (response) {
			var data = response.data;
			var status = response.status;
			Messages.add('warning', 'Unable to reset password for user "'+user.username+'".');
		});
		return this;
    };
    
    this.importUsers = function (file, uploadUrl) {
    	var uploadUrl = baseUrl + "importUsers";
        var fileFormData = new FormData();
        if(this.duplicatelist.length==0) {
        	fileFormData.append('file', file);
        } else {
        	uploadUrl = baseUrl + "importUsersAfterCheckingDuplicates";
        }
        fileFormData.append('encryptedFileName', hex_md5(file.name));
        fileFormData.append('overridenIds', this.getSelectedOverridenUserUids());
        fileFormData.append('skippedIds', this.getSkippedUserUids());
 
        var deffered = $q.defer();
        $http.post(uploadUrl, fileFormData, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}

        }).then(function (response) {
            deffered.resolve(response);
        }, function (response) {
            deffered.reject(response);
        });

        return deffered.promise;
    };
    
    this.removeUploadedFile =function(file){
    	 var removeUploadedFileUrl = baseUrl + "removeUploadedFile";
    	 var fileFormData = new FormData();
    	 fileFormData.append('encryptedFileName', hex_md5(file.name));
    	 var deffered = $q.defer();
         $http.post(removeUploadedFileUrl, fileFormData, {
             transformRequest: angular.identity,
             headers: {'Content-Type': undefined}

         }).then(function (response) {
             deffered.resolve(response);
         }, function (response) {
             deffered.reject(response);
         });

         return deffered.promise;
		
	}
    
    
    this.showMessage = function(tag, msg){
    	Messages.add(tag, msg);
    }
    
    this.getImportProgress = function(fileName, callback) {
		$http({
			method: 'GET',
			url: baseUrl+'importUsers/progress/' + fileName,
		}).then(function (response) {
			var data = response.data;
			var status = response.status;
			callback(data);
		});
		return this;
	};
	
	 this.getUploadProgress = function(fileName, callback) {
			$http({
				method: 'GET',
				url: baseUrl+'importUsers/preimportprogress/' + fileName,
			}).then(function (response) {
				var data = response.data;
				var status = response.status;
				callback(data);
			});
			return this;
		};
	
	
	this.duplicatelist = [];
	
	this.getSelectedOverridenUserUids = function(){
    	if(this.duplicatelist.length!=0){
    		var selectedUserUids = [];
    		for(var i=0; i<this.duplicatelist.length; i++) {
    			if(this.duplicatelist[i].isSelected)selectedUserUids.push(this.duplicatelist[i].username);
    		}
    		return selectedUserUids.toString();
    	} else {
    		return '';
    	}
    }
    
    this.getSkippedUserUids = function(){
    	if(this.duplicatelist.length!=0){
    		var skippedUserUids = [];
    		for(var i=0; i<this.duplicatelist.length; i++) {
    			if(!this.duplicatelist[i].isSelected)skippedUserUids.push(this.duplicatelist[i].username);
    		}
    		return skippedUserUids.toString();
    	} else {
    		return '';
    	}
    }
    
    this.setDuplicateList=function(duplicate){
    	if(duplicate){
    		this.duplicatelist=duplicate;
    		
    	} else {
    		this.duplicatelist=[];
    	}
    }
    
    this.exportAllUSers = function(callback){
    	this.exportUsers(1, callback);
		return this;
    }
    
    this.exportUsers = function(page, callback){
    	var that = this;
    	$http({
			method: 'GET',
			url: baseUrl+'users/export/' + page
			//url: 'json/users.json',
		}).then(function (response) {		
			
			console.log("retreiving " + response.data.rows + " users from page number " + response.data.currentPage);
			
			callback(response.data.result, response.data.currentPage, response.data.pageSize, response.data.totalItems);
			
		
			if((response.data.currentPage * response.data.pageSize) < response.data.totalItems){
				
				that.exportUsers(response.data.currentPage+1, callback);
			}
			
		}, function(response){
			// This is most likely due to usertoken timeout - TODO: Redirect to login webapp   
			console.log('Unable to search', response);
			var status = response.status;
			switch (status) {
				case 403: /* Forbidden */
					Messages.add('danger', 'Unable to seach! Forbidden...');
					break;
				case 404:  /* 404 No access */
					Messages.add('danger', 'Unable to search! No access...');
					break;
				case 409:  /* 409 Conflict - will prbably not occur here */
					Messages.add('danger', 'Search already exists...');
					break;
				default:
			    	Messages.add('danger', 'Search failed with error code: ' + status);
			}

		});
		return this;
    }

});