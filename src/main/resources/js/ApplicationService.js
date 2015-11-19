UseradminApp.service('Applications', function($http,Messages){

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
	this.selected = false;

	this.search = function() {
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

    this.get = function(id, callback) {
        console.log('Getting Application with id=', id);
        var that = this;
        $http({
            method: 'GET',
            url: baseUrl+'application/'+id+'/'
        }).success(function (data) {
            console.log('Got applicaton', data);
            that.application = data;
            if(callback) {
                callback(data);
            }
        });
        return this;
    };

    this.add = function(application, successCallback) {
        console.log('Adding application', application);
        var that = this;
        application.security = {};
        application.security.secret = application.secret;
        delete application.secret;
        $http({
            method: 'POST',
            url: baseUrl+'application/',
            data: application
        }).success(function (data) {
            Messages.add('success', 'application "'+application.name+'" was added successfully.');
            application.id = data.id;
            that.search(that.searchQuery);
            if(successCallback){
                successCallback();
            }
        }).error(function(data,status){
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
        return this;
    };

    this.save = function(application, successCallback) {
        console.log('Updating application', JSON.stringify(application));
        var that = this;
        if (application.hasOwnProperty(secret)) {
            application.security = {};
            application.security.secret = application.secret;
            delete application.secret;
        }
        $http({
            method: 'PUT',
            url: baseUrl+'application/'+application.id +'/',
            data: application
        }).success(function (data) {
            Messages.add('success', 'application "'+application.name+'" was updated successfully.');
            application.id = data.id;
            that.search(that.searchQuery);
            if(successCallback){
                successCallback();
            }
        }).error(function(data,status){
            console.log('application was not updated', data);
            switch (status) {
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
        return this;
    };

});