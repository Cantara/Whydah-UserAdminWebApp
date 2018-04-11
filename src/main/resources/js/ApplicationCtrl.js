UseradminApp.controller('ApplicationCtrl', function($scope, $http, $window, $routeParams, Users, Applications, ngProgressFactory, $interval, ConstantValues) {

	$scope.DEFCONS = ['DEFCON1', 'DEFCON2', 'DEFCON3', 'DEFCON4', 'DEFCON5'];
	
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
		angular.forEach(Applications.allSelectedItems, function(item, index){
			Applications.allSelectedItems[index] = [];
		});
		Applications.applyFilters();
	}
	
	$scope.onFiltersChanged = function(){
		
		
		Applications.applyFilters();
		
	}
	
	
	
	$scope.displayTagFilterModal = function(){
		$('#applicationTagModal').modal('show');
	
	}

	
	
	function init() {

		
		
		
		Applications.search('*');
		
		$scope.progressbar = ngProgressFactory.createInstance();
		$scope.progressbar.setParent(document.getElementById('progress'));

		
	}

//	if(typeof(Applications.list) != 'undefined' && Applications.list.length<1) {
//		init();
//	}
	
	init();

	$scope.activateApplicationDetail = function(id) {
		console.log('Activating application detail...', id);
		Applications.get(id, function(){
			//$scope.form.userDetail.$setPristine();
			$('#applicationdetail').modal('show');
		});
	}
	
	 var theIntervalUpdateLog;
	
	$scope.activateApplicationDetail2 = function(id) {
		console.log('Activating application detail...', id);
		Applications.get(id, function(){
			//$scope.form.userDetail.$setPristine();
			$('#applicationdetail2').modal('show').on("hidden.bs.modal", function () {
				 $interval.cancel(theIntervalUpdateLog);
				 $scope.form.applicationDetail.$cancel();
			});
			//fetch logs for this application
			 getLog(id);
			 $interval.cancel(theIntervalUpdateLog);
			 theIntervalUpdateLog = $interval(function(){
				 getLog(id);
	         }.bind(this), ConstantValues.clientsAutoUpdateLogInterval);
			
			
		});
	}
	
	function getLog(id){
		console.log("updating access log...");
		 Applications.getLog(id, function(){
			 console.log("access log updated");
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
        Applications.application.id=guid();
		Applications.application.tagList=[];
		Applications.application.organizationNames=[];
		Applications.application.roles=[];
		Applications.application.acl=[];
		Applications.application.security={};
		Applications.application.minimumDEFCONLevel= 'DEFCON5';
		Applications.application.security.minimumDEFCONLevel='DEFCON5';
		
		
		$scope.application = {isNew: true};
		//Users.userRoles = {};
		//$scope.form.applicationDetail.$setPristine();
		$('#applicationdetail2').modal('show');
		$scope.form.applicationDetail.$show();
		
	}
	
	

	// -- Should be in ApplicationdetailCtrl ---
	$scope.applicationProperties = [
	                                {value: 'id', readOnly: 'true'},
	                                //{value: 'id',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
	                                {value: 'name',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
	                                {value: 'defaultOrganizationName',  type: 'select',  minLength: 2, maxLength: 64, required: true, validationMsg:'Must be between 2-64 characters long.'},
	                                {value: 'defaultRoleName',  type: 'select',  minLength: 2, maxLength: 64, required: true, validationMsg:'Must be between 2-64 characters long.'},
	                                {value: 'description',    required:false, type: 'textarea'},
	                                {value: 'applicationUrl',     required: false, type: 'url', validationMsg: 'Must be valid URL.'},
	                                {value: 'logoUrl', required: false},
	                                {value: 'fullTokenApplication',    required: false, type: 'text'},
	                                {value: 'secret', minLength: 12, maxLength: 254, required: true, type: 'text', validationMsg:'Must be between 12-254 characters long. No spaces allowed.'},
	                                {value: 'roleNames', required: false, type: 'text', validationMsg:'Comma separated list of available role names'},
	                                {value: 'orgNames', required: false, type: 'text', validationMsg:'Comma separated list of available organization names'},
	                                {value: 'tags', required: false, type: 'text'},
	                                {value: 'whydahAdmin', type:'checkbox'},
	                                {value: 'whydahUASAccess', type:'checkbox'},
	                                {value: 'userTokenFilter', type:'checkbox'},
	                                {value: 'minimumDEFCONLevel', type:'select'}
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

	$scope.onImageSelected = function(){
	
		$scope.form.applicationDetail.$setDirty();
		
	}
	
	$scope.save = function() {
		
		
		// Make sure these $scope-values are properly connected to the view
		if($scope.form.applicationDetail.$valid){
			
			if($scope.image){
				Applications.application.logoUrl = angular.copy($scope.image.resized.dataURL);
			}
			
			
			if(Applications.application.isNew) {
				
				Applications.add(Applications.application, function(){
					delete Applications.application.isNew;
					$scope.form.applicationDetail.$setPristine();
					$scope.form.applicationDetail.$cancel();
					init();
					$scope.image = null;
					$('#applicationdetail2').modal('hide');
				});
			} else {
				
				Applications.save(Applications.application, function(){
					$scope.form.applicationDetail.$setPristine();
					$scope.form.applicationDetail.$cancel();
					$scope.image = null;
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
		if(theIntervalUpdateLog){
			$interval.cancel(theIntervalUpdateLog);
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
		$scope.form.applicationDetail.$setDirty();
	}
	
	$scope.removeAnAcl = function(index){
		Applications.application.acl.splice(index, 1);
		$scope.form.applicationDetail.$setDirty();
	}
	
	$scope.addANewAcl = function(){
		Applications.application.acl.push({"applicationId":"","applicationACLPath":"","accessRights":""});
		$scope.form.applicationDetail.$setDirty();
	}
	
	$scope.removeARole = function(index){
		Applications.application.roles.splice(index, 1);
		$scope.form.applicationDetail.$setDirty();
	}
	
	$scope.addANewRole = function(){
		Applications.application.roles.push({"id":"","name":""});
		$scope.form.applicationDetail.$setDirty();
	}
	
	$scope.removeAnOrg = function(index){
		Applications.application.organizationNames.splice(index, 1);
		$scope.form.applicationDetail.$setDirty();
	}
	
	$scope.addANewOrg = function(){
		Applications.application.organizationNames.push({"id":"","name":""});
		$scope.form.applicationDetail.$setDirty();
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