UseradminApp.service('Applications', function($http,Messages, $q, Application){

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

    this.searchAll = function() {
        console.log('Searching for applications...');
        var that = this;
        $http({
            method: 'GET',
            url: baseUrl+'applications/find/*'
        }).then(function (response) {
			var data = response.data;
			var status = response.status;
            that.list = data;
        }, function(response){
        	Messages.add('danger', 'Operation failed - Status code: ' + response.data.status + " - " +  response.data.message);
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

        }).then(function (response) {
        	console.log(response);
            deffered.resolve(response);
        }, function (response) {
        	console.log(response);
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
         }).then(function (response) {
			var data = response.data;
			var status = response.status;
            that.list = data;
            //get all tags as well
            $http({
                method: 'GET',
                url: baseUrl+'applicationtags'
             }).then(function(response){
            	 
            	
            	//TODO: receive filter history from server as well
                 var filterHistory = null;
                 that.allTags = response.data;
                 //apply filter to the list
                 that.initMenu(filterHistory);
                 
                 
             }, function(response){
            	 Messages.add('danger', 'Unable to get all tags - status code' + response.status);
             });
            
         }, function (response) {
        	 Messages.add('danger', 'Operation failed - Status code: ' + response.data.status + " - " +  response.data.message);

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
        }).then(function (response) {
        	var data = response.data;
            console.log('Got applicaton', data);
            that.application = new Application(data);
            that.application.applicationJson = JSON.stringify(data);
            that.application.applicationLog = new Object();
            that.application.tagList = that.allTags[id]? that.allTags[id]:[];
            callback(that.application);
            
           
        }, function(response){
        	Messages.add('danger', 'Operation failed - Status code: ' + response.data.status + " - " +  response.data.message);
        });
        return this;
    };
    
    this.getLog = function(id, callback) {
        console.log('Getting Application log for id=', id);
        var that = this;
        $http({
            method: 'GET',
            url: baseUrl+'applicationlog/'+id+'/'
        }).then(function (response) {
			var data = response.data;
			var status = response.status;
            console.log('Got applicaton log', data);
            that.application.applicationLog = data;
            if(callback) {
                 callback(that.application);
            }
        }, function(response){
        	Messages.add('danger', 'Operation failed - Status code: ' + response.data.status + " - " +  response.data.message);
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
            }).then(function (response) {
    			var data = response.data;
    			var status = response.status;
                Messages.add('success', 'application "' + application.name + '" was added successfully.');
                console.log(data);
                application.id = data.id;
                
                if (successCallback) {
                    successCallback();
                }
            }, function (response) {
            	Messages.add('danger', 'Operation failed - Status code: ' + response.data.status + " - " +  response.data.message);
             
            });
        }
        return this;
    };

    this.save = function(application, successCallback) {
        console.log('Updating application', JSON.stringify(application));
        var that = this;
     
        var postData = buildApplicationUpdate(application);
        var copy = angular.copy(postData);
        delete copy.timeout_unit;
        delete copy.timeout_number;
        delete copy.isNew;
        delete copy.app_oauth2_redirect;
        delete copy.app_sso_redirect;
        
        if (postData.hasOwnProperty('name')) {
            console.log("Save this json: " + JSON.stringify(copy));
            $http({
                method: 'PUT',
                url: baseUrl + 'application/' + application.id + '/',
                data: copy
            }).then(function (response) {
    			var data = response.data;
    			var status = response.status;
                Messages.add('success', 'application "' + application.name + '" was updated successfully.');
                
                if (successCallback) {
                    successCallback();
                }
             
                
            }, function (response) {
    			
            	Messages.add('danger', 'Operation failed - Status code: ' + response.data.status + " - " +  response.data.message);
    			
    		
            });
            
            
            
           
        }
        return this;
    };

    function buildApplicationUpdate(application) {
    	
    	if(application.hasOwnProperty('timeout_number') &&
                application.hasOwnProperty('timeout_unit')){
                var timeout = 0;
                if(application.timeout_unit === 'MONTH(S)'){
                    timeout = application.timeout_number * 30 * 24 * 60 * 60 * 1000;
                } else if(application.timeout_unit === 'DAY(S)'){
                    timeout = application.timeout_number * 24 * 60 * 60 * 1000;
                } else if(application.timeout_unit === 'HOUR(S)'){
                    timeout = application.timeout_number * 60 * 60 * 1000;
                } else if(application.timeout_unit === 'MINUTE(S)'){
                    timeout = application.timeout_number * 60 * 1000;
                } else if(application.timeout_unit === 'SECOND(S)'){
                    timeout = application.timeout_number * 1000;
                }

                if(timeout!=0){
                    application.security.maxSessionTimeoutSeconds = timeout;
                }



            }

            if(application.hasOwnProperty('app_sso_redirect')){

                var found = false;
                angular.forEach(application.acl, function(item, index){
                    if(item.accessRights.includes('SSO_REDIRECT')){
                        found = true;
                        item.applicationACLPath = application.app_sso_redirect;
                    }
                });

                if(!found){
                    application.acl.push({"applicationId":application.id,"applicationACLPath":application.app_sso_redirect,"accessRights":['READ', 'SSO_REDIRECT']});

                }
            }

            if(application.hasOwnProperty('app_oauth2_redirect')){
                var found = false;
                angular.forEach(application.acl, function(item, index){
                    if(item.accessRights.includes('OAUTH2_REDIRECT')){
                        found = true;
                        item.applicationACLPath = application.app_oauth2_redirect;
                    }
                });

                if(!found){
                    application.acl.push({"applicationId":application.id,"applicationACLPath":application.app_oauth2_redirect,"accessRights":['READ', 'OAUTH2_REDIRECT']});

                }

            }
    	
    	/*
        var postData = {};
        if (!application.hasOwnProperty('security')) {
        	application.security = {};  
        }
        if (application.hasOwnProperty('secret')) {
        	application.security.secret = application.secret;
        }
        if (application.hasOwnProperty('whydahAdmin')) {
        	application.security.whydahAdmin = application.whydahAdmin;
        }
        if (application.hasOwnProperty('whydahUASAccess')) {
        	application.security.whydahUASAccess = application.whydahUASAccess;
        }
        if (application.hasOwnProperty('userTokenFilter')) {
        	application.security.userTokenFilter = application.userTokenFilter;
        }
        if (application.hasOwnProperty('minimumDEFCONLevel')) {
        	application.security.minimumDEFCONLevel = application.minimumDEFCONLevel;
        }
        
      
        if (application.hasOwnProperty('acl')) {
        	 angular.forEach(application.acl, function(i, k){
        		  
        		  if (typeof i.accessRights === 'string') {
        			  var array = i.accessRights.split(',');
        			  i.accessRights = array;
                  }
        		  
             });
        	 console.log(application);
        }
        
        if(application.hasOwnProperty('timeout_number') && 
        		application.hasOwnProperty('timeout_unit')){
        	var timeout = 0;
        	if(application.timeout_unit === 'MONTH(S)'){
        		timeout = application.timeout_number * 30 * 24 * 60 * 60 * 1000;
        	} else if(application.timeout_unit === 'DAY(S)'){
        		timeout = application.timeout_number * 24 * 60 * 60 * 1000;
        	} else if(application.timeout_unit === 'HOUR(S)'){
        		timeout = application.timeout_number * 60 * 60 * 1000;
        	} else if(application.timeout_unit === 'MINUTE(S)'){
        		timeout = application.timeout_number * 60 * 1000;
        	} else if(application.timeout_unit === 'SECOND(S)'){
        		timeout = application.timeout_number * 1000;
        	} 
        	
        	if(timeout!=0){
        		application.security.maxSessionTimeoutSeconds = timeout;
        	}
        	
        }
        */
        
//        if (application.hasOwnProperty('roleNames')) {
//            if (typeof application.roleNames === 'string') {
//                application.roles = [];
//                var roleSplit = application.roleNames.split(",");
//                for (i = 0; i < roleSplit.length; i++) {
//                     var role = {};
//                     var item = roleSplit[i];
//                     role.id = item.trim();
//                     role.name = item.trim();
//                     application.roles.push(role);
//                }
//            }
//        }
//        if (application.hasOwnProperty('orgNames')) {
//            if (typeof application.orgNames === 'string') {
//                application.organizationNames = [];
//                var nameSplit = application.orgNames.split(",");
//                for (i = 0; i < nameSplit.length; i++) {
//                    var name = {};
//                    var item = nameSplit[i];
//                    name.id = item.trim();
//                    name.name = item.trim();
//                    application.organizationNames.push(name);
//                }
//            }
//        }
        if(application.hasOwnProperty('tagList')){
            application.tags ='';
            angular.forEach(application.tagList, function(i, k){
                application.tags += (application.tags===''?'':',') + (i.name.trim()!='UNNAMED'&&i.name.trim()!=''? (i.name.trim() + "_" + i.value.trim()) : i.value.trim());
            })
            console.log("TAGS" + application.tags);
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
                }).then(function (response) {
        			var data = response.data;
        			var status = response.status;
                    Messages.add('success', 'application "' + application.name + '" was updated successfully.');
                    application.id = data.id;
                    
                    if (successCallback) {
                        successCallback();
                    }
                }, function (response) {
                	Messages.add('danger', 'Operation failed - Status code: ' + response.data.status + " - " +  response.data.message);
                   
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
        /*
        if (application.hasOwnProperty(secret)) {
            application.security = {};
            application.security.secret = application.secret;
            delete application.secret;
        }*/
        $http({
            method: 'DELETE',
            url: baseUrl+'application/'+application.id +'/'
            //data: application
        }).then(function (response) {
			var data = response.data;
			var status = response.status;
            Messages.add('success', 'application "'+application.name+'" was deleted.');
            application.id = data.id;
            
            if(successCallback){
                successCallback();
            }
        }).error(function (response) {
        	Messages.add('danger', 'Operation failed - Status code: ' + response.data.status + " - " +  response.data.message);
           
        });
        return this;
    };
    
    this.getImportProgress = function(fileName, callback) {
		$http({
			method: 'GET',
			url: baseUrl+'importApps/progress/' + fileName,
		}).then(function (response) {
			var data = response.data;
			var status = response.status;
			callback(data);
		});
		return this;
	};
	

	//IMPLEMENTATION FOR TAG FILTERING
	
	this.tagFilterStatus = "No app filtered"; //status to show in UI
	this.allSelectedItems=[]; //selected tag objects [{id:'', label:'', appids:''}]
	this.allMenuSettings=[];  //settings for each menu
	this.allMenuDefaultTextSettings=[]; //settings for each menu
	this.filteredTagValues = []; //storage for filtered tags
	this.filteredAppIds = []; //filtered appids
	this.allTags =[]; //all tags for all applications
	this.allMenus=[]; //menu has this format [{"title":"UNNAMED", "menus":[ {id: 1, label: "David"}, {id: 2, label: "Jhon"}, {id: 3, label: "Danny"} ]}, {"title":"JUSRIDICTION", "menus":[ {id: 1, label: "Leif"}, {id: 2, label: "Jack"}, {id: 3, label: "Doe"} ]}]

	this.menuSettings = { 
			title :'',
			scrollable: true,
			enableSearch: true,
			keyboardControls: true,
			checkBoxes: false,
			styleActive: true,
			selectedToTop: true,
			buttonClasses: 'btn btn-default btn-sm',
			smartButtonTextProvider(selectionArray) { 
				return this.title + ' ' + (selectionArray.length) + " checked"; 
			}	
	};
	
	this.menuDefaultTextSettings = {buttonDefaultText: 'Select'};
	this.initMenu = function(filterHistory){
		
//		initialize JSON data for each menu FOR DEMOING 	
		
//		Applications.allMenus.push({"title":"UNNAMED", "menus":[ {id: 1, label: "David"}, {id: 2, label: "Jhon"}, {id: 3, label: "Danny"} ]});
//		Applications.allMenus.push({"title":"JUSRIDICTION", "menus":[ {id: 1, label: "Leif"}, {id: 2, label: "Jack"}, {id: 3, label: "Doe"} ]});
//		Applications.allMenus.push({"title":"OWNER", "menus":[ {id: 1, label: "Daniel"}, {id: 2, label: "Tom"}, {id: 3, label: "Ken"} ]});
//		Applications.allMenus.push({"title":"COMPANY", "menus":[ {id: 1, label: "Joe"}, {id: 2, label: "Jewish"}, {id: 3, label: "Ben"} ]});		
		
		
		//initialize the menu with data
		this.allMenus = [];
		var that = this;
		
		angular.forEach(this.list, function(app, appIndex){
			
			
			
			if(that.allTags[app.id]){
				
			
				angular.forEach(that.allTags[app.id], function(item, index){
					if(that.allMenus.length ==0){
						that.allMenus.push({title: item.name, menus: []});
					}
					
					
					
					for (var mindex = 0, len = that.allMenus.length; mindex < len; mindex++) {
						var mitem = that.allMenus[mindex];
						var menuFound = false;
						if(mitem["title"]===item.name){ //found the name in the main menu
							
							menuFound = true;
							//check if the value existing inside the sub-menus
							var found = false;
							
							for (var smindex = 0, slen = mitem.menus.length; smindex < slen; smindex++) {
								var smitem = mitem.menus[smindex];
								
								if(smitem["label"]===item.value){
									
									smitem.appids.push(app.id); //store appid
									found = true;
									break;
								}	
							}
							
							if(!found){
								var miObj = {id: mitem.menus.length, label: item.value, appids:[]};
								miObj.appids.push(app.id);
								mitem.menus.push(miObj);
							}
							break;
						}
						
					}
					
					if(!menuFound){
						var menu = {title: item.name, menus: []};
						var miObj = {id: 0, label: item.value, appids:[]};
						miObj.appids.push(app.id);
						menu.menus.push(miObj);
						that.allMenus.push(menu);
					}
					
				});
				
				
			}
		
		});
		
		//apply settings
		
		angular.forEach(this.allMenus, function(item, index){
			
			that.allSelectedItems[index]=[];
			that.allMenuSettings[index] = angular.copy(that.menuSettings);
			that.allMenuSettings[index].title = item.title;
			that.allMenuDefaultTextSettings[index] = angular.copy(that.menuDefaultTextSettings);
			that.allMenuDefaultTextSettings[index].buttonDefaultText = item.title;
		});
		
		
		//now apply tag filters from the history selection name-value format [{name:'name1', value:'value1'}, {name:'name2', value:'value2'}]
		//from local we already have filteredAppIds
		

		this.readFilteredTags(this.filteredTagValues);

		//from server (or local storage) we have filterHistory same above format
		if(filterHistory!=null){
			//do something
			this.readFilteredTags(filterHistory);
		}
		
		
	};
	
	this.applyFilters = function(){
		
		this.filteredAppIds = [];
		this.filteredTagValues =[];
		var that = this;
		//reload the list
		angular.forEach(this.allSelectedItems, function(item, index){
			if(item.length>0){
				
				
				var name = that.allMenus[index].title;
				
				for (var i = 0, len = item.length; i < len; i++) {
					
					that.filteredTagValues.push({name: name, value: item[i].label});
					
					for (var j = 0, jlen = item[i].appids.length; j < jlen; j++) {
						
						if(!that.filteredAppIds.contains(item[i].appids[j])){
							that.filteredAppIds.push(item[i].appids[j]);
						}
					}
				}
			}
		});
		
		console.log("FILTERED APP_IDS: " + that.filteredAppIds);
	
		
		angular.forEach(this.list, function(item, index){
			
			if(that.filteredAppIds.contains(item.id)){
				item.isFiltered = true;
			} else {
				item.isFiltered = false;
			}
		});
		
		if(this.filteredAppIds.length>0){
			this.tagFilterStatus = this.filteredAppIds.length + " app(s) filtered"
		} else {
			this.tagFilterStatus = "No app filtered";
		}
	}

	this.readFilteredTags = function(arrayOfFilteredTags){
		var array = angular.copy(arrayOfFilteredTags);
		//read all filter history
		var that = this;
		angular.forEach(array, function(tag, i){
			
			var index = that.allMenus.map(function(e) { return e.title; }).indexOf(tag.name);
			
			//get the menu, now apply selection for the value tag.value
			angular.forEach(that.allMenus[index].menus, function(menuitem, mi){
				if(menuitem.label===tag.value){
					
					that.allSelectedItems[index].push(angular.copy(menuitem));
				}
			});
			
		});
		

		//affect filters to UI 
		this.applyFilters();
	}

});