UseradminApp.service('Applications', function($http){

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

});