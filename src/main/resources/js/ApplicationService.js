UseradminApp.service('Applications', function($http,Messages, $q){

    var defaultlist = [
        {
            "id": "1",
            "name": "UserAdmin",
            "defaultRole": "UserAdmin",
            "defaultOrgid": "Whydah",
            "availableOrgIds": null
        },
        {
            "id": "2",
            "name": "Mobilefirst",
            "defaultRole": "client",
            "defaultOrgid": "Altran",
            "availableOrgIds": null
        },
        {
            "id": "3",
            "name": "Whydah",
            "defaultRole": "developer",
            "defaultOrgid": "Whydah",
            "availableOrgIds": null
        }
    ];

    var wishlist = {
        applicationId: 1,
        applicationName: 'name',
        defaultRole: 'role',
        defaultRoleValue: 'value',
        defaultOrg: 'org1',
        organisations: [
            'org1',
            'org2',
            'org3'
        ]
    }

    this.list = [];
    this.duplicatelist = [];

    this.selected = false;

    this.search2 = function() {
        console.log('Searching for applications...');
        var that = this;
        $http({
            method: 'GET',
            url: baseUrl + 'applications'
        }).success(function (data) {
            that.list = data;
        });
        return this;
    };
    
    this.importApps = function (file, uploadUrl) {
        var fileFormData = new FormData();
        fileFormData.append('file', file);
        fileFormData.append('overridenIds', this.getSelectedOverridenApIds());
        fileFormData.append('skippedIds', this.getSkippedApIds());
        var deffered = $q.defer();
        $http.post(uploadUrl, fileFormData, {
            transformRequest: angular.identity,
            headers: {'Content-Type': undefined}

        }).success(function (response) {
            deffered.resolve(response);
        }).error(function (response) {
            deffered.reject(response);
        });

        return deffered.promise;
    };
    
    this.getSelectedList = function(){
    	var selectedApps = [];
    	for(var i=0; i<this.list.length; i++) {
			if(this.list[i].isSelected)selectedApps.push(this.list[i]);
		}
    	return selectedApps;
    }
    
    this.getSelectedOverridenApIds = function(){
    	if(this.duplicatelist.length!=0){
    		var selectedAppIds = [];
    		for(var i=0; i<this.duplicatelist.length; i++) {
    			if(this.duplicatelist[i].isSelected)selectedAppIds.push(this.duplicatelist[i].id);
    		}
    		return selectedAppIds.toString();
    	} else {
    		return '';
    	}
    }
    
    this.getSkippedApIds = function(){
    	if(this.duplicatelist.length!=0){
    		var skippedAppIds = [];
    		for(var i=0; i<this.duplicatelist.length; i++) {
    			if(!this.duplicatelist[i].isSelected)skippedAppIds.push(this.duplicatelist[i].id);
    		}
    		return skippedAppIds.toString();
    	} else {
    		return '';
    	}
    }
    
    this.setDuplicateList=function(duplicateIds){
    	if(duplicateIds){
    		var that = this;
    		that.duplicatelist=[];
    		angular.forEach(this.list, function(i, k){
    			var newCloneApp = angular.copy(i);
    			if(duplicateIds.indexOf(newCloneApp.id) !== -1){
    				that.duplicatelist.push(newCloneApp); 
    			}
    		});
    	} else {
    		this.duplicatelist=[];
    	}
    }
    
     this.search = function(searchQuery) {
        console.log('Searching for applications...');
        this.searchQuery = searchQuery || '*';
         var that = this;
         $http({
            method: 'GET',
            url: baseUrl+'applications/find/'+this.searchQuery
            //url: 'json/users.json',
         }).success(function (data) {
             that.list = data;
        	
         }).error(function(data,status){
         // This is most likely due to usertoken timeout - TODO: Redirect to login webapp
         console.log('Unable to search', data);
         switch (status) {
            case 403: // Forbidden
               Messages.add('danger', 'Unable to seach! Forbidden...');
               break;
            case 404:  // 404 No access
               Messages.add('danger', 'Unable to search! No access...');
               break;
            case 409:  // 409 Conflict - will prbably not occur here
               Messages.add('danger', 'Search already exists...');
               break;
            default:
               Messages.add('danger', 'Search failed with error code: ' + status);
          }

          });
  
         return this;
     };

    function buildRoleNames(application) {
        var roleNames = [];
        if (application.hasOwnProperty("roles")) {
            if (application.roles && application.roles.length) {
                for (var i = 0; i < application.roles.length; i++) {
                    var role = application.roles[i];
                    roleNames.push(role.name);
                }
            }
        }
        return roleNames;
    }
    function buildOrgNames(application) {
        var orgNames = [];
        if (application.hasOwnProperty("organizationNames")) {
            if (application.organizationNames && application.organizationNames.length) {
                for (var i = 0; i < application.organizationNames.length; i++) {
                    var org = application.organizationNames[i];
                    orgNames.push(org.name);
                }
            }
        }
        return orgNames;
    }

    this.get = function(id, callback) {
        console.log('Getting Application with id=', id);
        var that = this;
        $http({
            method: 'GET',
            url: baseUrl+'application/'+id+'/'
        }).success(function (data) {
            console.log('Got applicaton', data);
            that.application = data;
            that.application.secret = data.security.secret;
            that.application.applicationJson = JSON.stringify(data);
            that.application.roleNames = buildRoleNames(that.application);
            that.application.orgNames = buildOrgNames(that.application);
            if(callback) {
                callback(that.application);
            }
        });
        return this;
    };


    this.getLog = function(id, callback) {
        console.log('Getting Application log for id=', id);
        var that = this;
        $http({
            method: 'GET',
            url: baseUrl+'observe/statistics/'+id+'/usersession'
        }).success(function (data) {
            console.log('Got applicaton log', data);
            if(callback) {
                callback(data);
            }
        });
        return this;
    };

    this.add = function(application, successCallback) {
        console.log('Adding application', application);
        var that = this;

        //application.security = {};
        //application.security.secret = application.secret;
        //delete application.secret;
        var postData = buildApplicationUpdate(application);
        if (postData.hasOwnProperty('name')) {
            $http({
                method: 'POST',
                url: baseUrl + 'application/',
                data: postData
            }).success(function (data) {
                Messages.add('success', 'application "' + application.name + '" was added successfully.');
                application.id = data.id;
                that.search(that.searchQuery);
                if (successCallback) {
                    successCallback();
                }
            }).error(function (data, status) {
                console.log('application was not added', data);
                switch (status) {
                    case 403:
                        Messages.add('danger', 'application was not added! No access...');
                        break;
                    case 404:  /* 404 No access */
                        Messages.add('danger', 'application was not added! No access...');
                        break;
                    case 409:  /* 409 Conflict - user exists or was double posted */
                        Messages.add('danger', 'application was not added! Already exists...');
                        break;
                    default:
                        Messages.add('danger', 'application was not added and! Try again later...');
                }
                $scope.activateTimeoutModal();
            });
        }
        return this;
    };

    this.save = function(application, successCallback) {
        console.log('Updating application', JSON.stringify(application));
        var that = this;
        var postData = buildApplicationUpdate(application);
        if (postData.hasOwnProperty('name')) {
            console.log("Save this json: " + JSON.stringify(postData));
            $http({
                method: 'PUT',
                url: baseUrl + 'application/' + application.id + '/',
                data: postData
            }).success(function (data) {
                Messages.add('success', 'application "' + application.name + '" was updated successfully.');
                application.id = data.id;
                that.search(that.searchQuery);
                if (successCallback) {
                    successCallback();
                }
            }).error(function (data, status) {
                console.log('application was not updated', data);
                switch (status) {
                    case 400:
                        Messages.add('danger','application was not updated! Please validate your form and applicationjson input.');
                        break;
                    case 403:
                        Messages.add('danger', 'application was not updated! No access...');
                        break;
                    case 404:  /* 404 No access */
                        Messages.add('danger', 'application was not updated! No access...');
                        break;
                    case 409:  /* 409 Conflict - user exists or was double posted */
                        Messages.add('danger', 'application was not updated! Already exists...');
                        break;
                    default:
                        Messages.add('danger', 'application was not updated and! Try again later...');
                }
                $scope.activateTimeoutModal();
            });
        }
        return this;
    };

    function buildApplicationUpdate(application) {
        var postData = {};
        if (application.hasOwnProperty('secret')) {
            if (application.hasOwnProperty('security')) {
                application.security.secret = application.secret;
            } else {
                application.security = {};
                application.security.secret = application.secret;
            }
            delete application.secret;
        }
        if (application.hasOwnProperty('roleNames')) {
            if (typeof application.roleNames === 'string') {
                application.roles = [];
                var roleSplit = application.roleNames.split(",");
                for (i = 0; i < roleSplit.length; i++) {
                     var role = {};
                     var item = roleSplit[i];
                     role.id = item.trim();
                     role.name = item.trim();
                     application.roles.push(role);
                }
            }
        }
        if (application.hasOwnProperty('orgNames')) {
            if (typeof application.orgNames === 'string') {
                application.organizationNames = [];
                var nameSplit = application.orgNames.split(",");
                for (i = 0; i < nameSplit.length; i++) {
                    var name = {};
                    var item = nameSplit[i];
                    name.id = item.trim();
                    name.name = item.trim();
                    application.organizationNames.push(name);
                }
            }
        }
        return application;
    }
    
    this.showMessage = function(tag, msg){
    	Messages.add(tag, msg);
    }

    this.saveFromJson = function(application, successCallback) {
        console.log('Updating application from json', JSON.stringify(application.applicationJson));
        var that = this;
        var postData = {};
        if (application.applicationJson) {
            try {
                postData = JSON.parse(application.applicationJson);

                console.log("Save this json: " + JSON.stringify(postData));
                $http({
                    method: 'PUT',
                    url: baseUrl + 'application/' + application.id + '/',
                    data: postData
                }).success(function (data) {
                    Messages.add('success', 'application "' + application.name + '" was updated successfully.');
                    application.id = data.id;
                    that.search(that.searchQuery);
                    if (successCallback) {
                        successCallback();
                    }
                }).error(function (data, status) {
                    console.log('application was not updated', data);
                    switch (status) {
                        case 400:
                            Messages.add('danger','application was not updated! Please validate your form and applicationjson input.');
                            break;
                        case 403:
                            Messages.add('danger', 'application was not updated! No access...');
                            break;
                        case 404:  /* 404 No access */
                            Messages.add('danger', 'application was not updated! No access...');
                            break;
                        case 409:  /* 409 Conflict - user exists or was double posted */
                            Messages.add('danger', 'application was not updated! Already exists...');
                            break;
                        default:
                            Messages.add('danger', 'application was not updated and! Try again later...');
                    }
                    $scope.activateTimeoutModal();
                });
            } catch (e) {
                Messages.add('danger', 'Could not parse applicationJson to JSON object.');
            }
        } else {
            Messages.add('danger', 'Could not update application. ApplcationJson is missing.');
        }

        return this;
    };

    this.delete = function(application, successCallback) {
        console.log('Deleting application', JSON.stringify(application));
        var that = this;
        if (application.hasOwnProperty(secret)) {
            application.security = {};
            application.security.secret = application.secret;
            delete application.secret;
        }
        $http({
            method: 'DELETE',
            url: baseUrl+'application/'+application.id +'/'
            //data: application
        }).success(function (data) {
            Messages.add('success', 'application "'+application.name+'" was deleted.');
            application.id = data.id;
            that.search(that.searchQuery);
            if(successCallback){
                successCallback();
            }
        }).error(function(data,status){
            console.log('application was not deleted.', data, status);
            switch (status) {
                case 403:
                    Messages.add('danger', 'application was not deleted! No access...');
                    break;
                case 404:  /* 404 No access */
                    Messages.add('danger', 'application was not deleted! No access...');
                    break;
                case 409:  /* 409 Conflict - user exists or was double posted */
                    Messages.add('danger', 'application was not deleted! Already exists...');
                    break;
                default:
                    Messages.add('danger', 'application was not deleted and! Try again later...');
            }
            $scope.activateTimeoutModal();
        });
        return this;
    };
    
    this.getImportProgress = function(fileName, callback) {
		$http({
			method: 'GET',
			url: baseUrl+'importApps/progress/' + fileName,
		}).success(function (data) {
			callback(data);
		});
		return this;
	};

});