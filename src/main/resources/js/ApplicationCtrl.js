UseradminApp.controller('ApplicationCtrl', function($scope, $http, $window, $routeParams, Users, Applications, ngProgressFactory, $interval) {

	$scope.session.activeTab = 'application';

	$scope.users = Users;
	$scope.applications = Applications;
	$scope.displayCollectionList = [];

	$scope.form = {};
	$scope.orderByColumn = 'name';
	$scope.orderReverse = false;
	

	$scope.changeOrder = function(orderByColumn) {
		$scope.orderByColumn = orderByColumn;
		$scope.orderReverse = !$scope.orderReverse;
	}

	$scope.searchApps = function() {
		Applications.search($scope.searchQuery);

	}

	$('body').on('click', '.disabled', function(e) {
		e.preventDefault();
		return false;
	});

	
	$scope.tagFilterSettings = { 
			
			scrollable: true,
			enableSearch: true,
			keyboardControls: true,
			checkBoxes: false,
			styleActive: true,
			selectedToTop: true,
			buttonClasses: 'btn btn-default btn-sm',
			smartButtonTextProvider(selectionArray) { 
				return "UNNAMED_" + (selectionArray.length) + " checked"; 
			}
			
	};
	
	Array.prototype.contains = function(element){
	    return this.indexOf(element) > -1;
	};
	
	$scope.clearAllFilters = function(){
		angular.forEach($scope.allSelectedItems, function(item, index){
			$scope.allSelectedItems[index] = [];
		});
		applyFilters();
	}
	
	$scope.onFiltersChanged = function(){
		
		
		applyFilters();
		
	}
	
	
	var filteredTagValues = [];
	
	var filteredAppIds = [];
	
	var applyFilters = function(){
		
		filteredAppIds = [];
		filteredTagValues =[];
		
		//reload the list
		angular.forEach($scope.allSelectedItems, function(item, index){
			if(item.length>0){
				
				
				var name = $scope.allMenus[index].title;
				
				for (var i = 0, len = item.length; i < len; i++) {
					
					filteredTagValues.push({name: name, value: item[i].label});
					
					for (var j = 0, jlen = item[i].appids.length; j < jlen; j++) {
						
						if(!filteredAppIds.contains(item[i].appids[j])){
							filteredAppIds.push(item[i].appids[j]);
						}
					}
				}
			}
		});
		
		console.log("FILTERED APP_IDS: " + filteredAppIds);
	
		
		angular.forEach(Applications.list, function(item, index){
			
			if(filteredAppIds.contains(item.id)){
				item.isFiltered = true;
			} else {
				item.isFiltered = false;
			}
		});
		
		if(filteredAppIds.length>0){
		   $scope.tagFilterStatus = filteredAppIds.length + " app(s) filtered"
		} else {
		   $scope.tagFilterStatus = "No app filtered";
		}
	}
	
	$scope.tagFilterStatus = "No app filtered";
	
	$scope.displayTagFilterModal = function(){
		$('#applicationTagModal').modal('show');
	
	}

	//tag menus initialization
	$scope.allSelectedItems=[];
	$scope.allMenuSettings=[];
	$scope.allMenuDefaultTextSettings=[];
	$scope.allMenus=[];
	$scope.noTagAppIds=[];
	var menuSettings = { 
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
	
	var menuDefaultTextSettings = {buttonDefaultText: 'Select'};
	var initMenu = function(filterHistory){
		
		//initialize JSON data for each menu FOR DEMOING
//		$scope.allMenus.push({"title":"UNNAMED", "menus":[ {id: 1, label: "David"}, {id: 2, label: "Jhon"}, {id: 3, label: "Danny"} ]});
//		$scope.allMenus.push({"title":"JUSRIDICTION", "menus":[ {id: 1, label: "Leif"}, {id: 2, label: "Jack"}, {id: 3, label: "Doe"} ]});
//		$scope.allMenus.push({"title":"OWNER", "menus":[ {id: 1, label: "Daniel"}, {id: 2, label: "Tom"}, {id: 3, label: "Ken"} ]});
//		$scope.allMenus.push({"title":"COMPANY", "menus":[ {id: 1, label: "Joe"}, {id: 2, label: "Jewish"}, {id: 3, label: "Ben"} ]});		
		
		
		//initialize the menu with data
		$scope.allMenus = [];
		angular.forEach(Applications.list, function(app, appIndex){
			
			//if this application has tag list
			
			if(Applications.allTags[app.id]){
				
				
				angular.forEach(Applications.allTags[app.id], function(item, index){
					if($scope.allMenus.length ==0){
						$scope.allMenus.push({title: item.name, menus: []});
					}
					
					
					
					for (var mindex = 0, len = $scope.allMenus.length; mindex < len; mindex++) {
						var mitem = $scope.allMenus[mindex];
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
						$scope.allMenus.push(menu);
					}
					
				});
				
				
			} else {
				//no-tag applications 
				$scope.noTagAppIds.push(app.id);
			}
		
		});
		
		//apply settings
		angular.forEach($scope.allMenus, function(item, index){
			
			$scope.allSelectedItems[index]=[];
			$scope.allMenuSettings[index] = angular.copy(menuSettings);
			$scope.allMenuSettings[index].title = item.title;
			$scope.allMenuDefaultTextSettings[index] = angular.copy(menuDefaultTextSettings);
			$scope.allMenuDefaultTextSettings[index].buttonDefaultText = item.title;
		});
		
		
		//now apply tag filters from the history selection name-value format [{name:'name1', value:'value1'}, {name:'name2', value:'value2'}]
		//from local we already have filteredAppIds
		

		readFilteredTags(filteredTagValues);

		//from server (or local storage) we have filterHistory same above format
		if(filterHistory!=null){
			//do something
			readFilteredTags(filterHistory);
		}
		
		
	};
	
	
	var readFilteredTags = function(arrayOfFilteredTags){
		var array = angular.copy(arrayOfFilteredTags);
		//read all filter history
		angular.forEach(array, function(tag, i){
			
			var index = $scope.allMenus.map(function(e) { return e.title; }).indexOf(tag.name);
			
			//get the menu, now apply selection for the value tag.value
			angular.forEach($scope.allMenus[index].menus, function(menuitem, mi){
				if(menuitem.label===tag.value){
					
					$scope.allSelectedItems[index].push(angular.copy(menuitem));
				}
			});
			
		});
		
		angular.forEach($scope.allSelectedItems, function(tag, i){
			
			console.log(tag);
			
		});

		//affect filters to UI 
		applyFilters();
	}
	
	function init() {

		
		
		
		Applications.search('*', initMenu);
		
		
		$scope.progressbar = ngProgressFactory.createInstance();
		$scope.progressbar.setParent(document.getElementById('progress'));


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
			//$scope.prettifyJson();
		});
	}

	$scope.activateApplicationLog = function(id) {
		console.log('Activating application log...', id);
		Applications.showMessage('info', "Loading application history. Please wait a moment.");
		Applications.get(id, function(){

			Applications.getLog(id, function(){

				$('#applicationLog').modal('show');

			});

		});


	}

	$scope.exportSelectedApps=function(){
		var blob = new Blob([angular.toJson(Applications.getSelectedList(), true)], {type: "text/plain;charset=utf-8"});
		saveAs(blob, "applications.json");
	}

	$scope.exportApps = function() {
		init();
		var blob = new Blob([angular.toJson(Applications.list, true)], {type: "text/plain;charset=utf-8"});
		saveAs(blob, "applications.json");
	}



	$scope.newApplicationDetail = function() {
		Applications.application = {isNew: true};
		Applications.application.tagList=[];
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
	                                {value: 'orgNames', required: false, type: 'text', validationMsg:'Comma separated list of available organization names'},
	                                {value: 'tags', required: false, type: 'text'}
	                                ];

	$scope.applicationJsonProperties = [
	                                    {value: 'applicationJson', required: false, type: 'json', validationMsg:'The input must be valid json. Recomend http://jsonlint.com for manual validation.'},
	                                    ];

	$scope.applicationLogProperties = [
	                                   {value: 'applicationLog', required: false, type: 'json', validationMsg:'The input must be valid json. Recomend http://jsonlint.com for manual validation.'},
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
				orgNames: 'Available organization names',
				tags: 'Tags',
				applicationLog: 'Log',
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
					init();
				});
			} else {
				
				
				Applications.save(Applications.application, function(){
		
					$scope.form.applicationDetail.$setPristine();
					init();
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
					init();
				});
			} else {
				Applications.saveFromJson(Applications.application, function(){
					$scope.form.applicationJson.$setPristine();
					init();
				});
			}
		} else {
			console.log('Tried to save an invalid form.');
		}
	}

//	$scope.prettifyJson = function() {
//	try {
//	var jsonObject = JSON.parse($scope.applications.application.applicationJson);
//	var prettifiedJson = JSON.stringify(jsonObject, undefined, 2);
//	$scope.applications.application.applicationJson = prettifiedJson;
//	}catch (e) {
//	Messages.add('danger', 'Could not prettify applicationJson. Error while parsing');
//	}
//	}
	
	  var getUUID = function(){
       	 var d = new Date().getTime();
            var uuid = 'xxxxxxxxxxxxxxxxxxxxxxxxx'.replace(/[x]/g, function(c) {
                var r = (d + Math.random()*16)%16 | 0;
                d = Math.floor(d/16);
                return (c=='x' ? r : (r&0x3|0x8)).toString(16);
            });
            return uuid;
       }

	$scope.delete = function() {
		var deleteUser = $window.confirm('Are you absolutely sure you want to delete '+ Applications.application.name +'?');

		if (deleteUser) {
			Applications.delete(Applications.application, function(){
				$scope.form.applicationDetail.$setPristine();
				init();
			});
		}
	}

	var theInterval;
	$scope.$on('$destroy', function () {
		if(theInterval){
			$interval.cancel(theInterval);
		}
	});

	$scope.importApps = function(){
		Applications.setDuplicateList(null);
		$('#applicationImport').modal('show');
		
	}

	$scope.removeTag = function(index){
		Applications.application.tagList.splice(index, 1);
		
		$scope.form.applicationDetail.$setDirty();
	}

	$scope.addANewTag = function(){
		Applications.application.tagList.push({"name":"","value":""});
		

	}


	$scope.uploadFile = function () {
		var file = $scope.myFile;
		if(file){



			var uploadUrl = baseUrl + "importApps", //Url of web service
			promise = Applications.importApps(file, uploadUrl);

			if(Applications.duplicatelist && Applications.duplicatelist.length>0){
				$scope.importing = true;
				console.log("Ready to import apps now.");
				console.log("Timer has been started, to update import progress.");
				//ask server for the progress each 2 secs
				theInterval = $interval(function(){
					Applications.getImportProgress(hex_md5(file.name), function(data){
						if(data>0){
							//can close the modal now
							$('#applicationImport').modal('hide');
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
						Applications.showMessage('danger','An error has occurred: ' + response.data.result);
						return;
					} else if(/^ok/i.test(response.data.result)===true){
						Applications.showMessage('success', "Imported successfully");
						$('#applicationImport').modal('hide');
						//refresh the list
						init();
						return;
					} else {

						Applications.setDuplicateList(response.data.result);
						return;
					}
				}
			}, function (response) {
				Applications.showMessage('danger','An error has occurred: ' + response.data.result);
			})
		}

	}

});