UseradminApp.controller('ApplicationCtrl', function($scope, $http, $window, $routeParams, Users, Applications, ngProgressFactory, $interval, ConstantValues, ModalService, Application) {

	$scope.DEFCONS = ['DEFCON1', 'DEFCON2', 'DEFCON3', 'DEFCON4', 'DEFCON5'];
	$scope.ACCESS_RIGHTS = ['READ', 'WRITE', 'CREATE', 'DELETE', 'SSO_REDIRECT', 'OAUTH2_REDIRECT'];
	$scope.SESSION_TIMEOUT_UNIT = ['MONTH(S)', 'DAY(S)', 'HOUR(S)', 'MINUTE(S)', 'SECOND(S)'];
	
	$scope.session.activeTab = 'application';

	$scope.users = Users;
	$scope.applications = Applications;
	
	$scope.displayCollectionList = [];
	$scope.displayActivityList = [];

	$scope.form = {};
	$scope.orderByColumn = 'name';
	$scope.orderReverse = false;
	
	$scope.format = 'yyyy/MM/dd';
	$scope.form = {};
	$scope.from_date = addDays(new Date(), -1);
	$scope.to_date = addDays(new Date(), 1);
	
	function addDays(date, days) {
		 var result = new Date(date);
		 result.setDate(result.getDate() + days);
		 return result;
	}
	
	function dateToString(date){
		 return date.getFullYear() + '-' + (date.getMonth() + 1) + '-' + date.getDate();
	}
	
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
	
	$scope.queryLogs = function(){
		
		getLog(Applications.application.id, function(){
			 console.log("access log updated");
		 });
		 
	}
	
	$scope.displayTagFilterModal = function(){
		$('#applicationTagModal').modal('show');
	
	}
	
	$scope.editLogo = function(){
    	
    	if($scope.form.applicationDetail.$visible) {

        ModalService.showModal({

            templateUrl: "template/template_logo_edit.html",
            controller: function( $element){
                this.logoUrl =  $scope.application.logoUrl;
                this.close = function () {
                    
                    if(this.logoUrl) {
                        $scope.application.logoUrl = this.logoUrl;
                        $scope.form.applicationDetail.$setDirty();
                    }
                }

                this.cancel = function(){
                    $element.modal('hide');
                }
            },
            controllerAs : "modalCtrl"

        }).then(function(modal) {
            // The modal object has the element built, if this is a bootstrap modal
            // you can call 'modal' to show it, if it's a custom modal just show or hide
            // it as you need to.
            modal.element.modal();
            //modal.close.then(function(result) {
                //console.log('OK');
                //console.log(result);
            //});
        });
        
    	}


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
	
	//var theIntervalUpdateLog;
	
	$scope.activateApplicationDetail2 = function(id) {
		console.log('Activating application detail...', id);
		
		Applications.get(id, function(){
			$scope.application = Applications.application;
			//$scope.form.userDetail.$setPristine();
			
			$('#applicationdetail2').modal('show').on("hidden.bs.modal", function () {
				 //$interval.cancel(theIntervalUpdateLog);
				 $scope.form.applicationDetail.$cancel();
			});
			 //fetch logs for this application
			 getLog(id,  function(){
				 console.log("access log updated");
			 });
			 
			 /*
			 $interval.cancel(theIntervalUpdateLog);
			 theIntervalUpdateLog = $interval(function(){
				 getLog(id, function(){
					 console.log("access log updated");
				 });
				 
	         }.bind(this), ConstantValues.clientsAutoUpdateLogInterval);*/
			
			
		});
	}
	
	function getLog(id, callback){
		console.log("updating access log...");
		Applications.showMessage('info', "Loading application history. Please wait a moment.");
		Applications.getLog(id, dateToString($scope.from_date), dateToString($scope.to_date), callback);
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
		
		Applications.get(id, function(){
			getLog(id, function(){

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
		
		Applications.application = new Application();
		$scope.application = Applications.application;
		
		/*
		Applications.application = {isNew: true};
        Applications.application.id=uuid();
		Applications.application.tagList=[];
		Applications.application.organizationNames=[];
		Applications.application.roles=[];
		Applications.application.acl=[];
		Applications.application.security={};
		Applications.application.minimumDEFCONLevel= 'DEFCON5';
		Applications.application.security.minimumDEFCONLevel='DEFCON5';
		Applications.application.secret=uuid();
		Applications.application.timeout_number=6; //6 months
		Applications.application.timeout_unit='MONTH(S)';
		
		$scope.application = {isNew: true};
		//Users.userRoles = {};
		//$scope.form.applicationDetail.$setPristine();
		*/
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
	                                {value: 'minimumDEFCONLevel', type:'select'},
	                                {value: 'timeout_number', type:'number'},
	                                {value: 'timeout_unit', type:'select'},
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
				applicationLog: 'Log'
			}
	}

	$scope.onImageSelected = function(){
	
		$scope.form.applicationDetail.$setDirty();
		
	}
	
	$scope.save = function() {
		
		if(!$scope.form.applicationDetail.$dirty) {
			return;
		}
		
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
//		if(theIntervalUpdateLog){
//			$interval.cancel(theIntervalUpdateLog);
//		}
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
		Applications.application.acl.push({"applicationId":Applications.application.id,"applicationACLPath":"","accessRights":['READ']});
		$scope.form.applicationDetail.$setDirty();
	}
	
	$scope.removeARole = function(index){
		Applications.application.roles.splice(index, 1);
		$scope.form.applicationDetail.$setDirty();
	}
	
	$scope.addANewRole = function(){
		var id = Applications.application.roles.length;
		angular.forEach(Applications.application.roles, function(item, index){
			if(item.id===id){
				id = id + 1;
			}
		});
		Applications.application.roles.push({"id":id,"name":""});
		$scope.form.applicationDetail.$setDirty();
	}
	
	$scope.removeAnOrg = function(index){
		Applications.application.organizationNames.splice(index, 1);
		$scope.form.applicationDetail.$setDirty();
	}
	
	$scope.addANewOrg = function(){
		var id = Applications.application.organizationNames.length;
		angular.forEach(Applications.application.organizationNames, function(item, index){
			if(item.id===id){
				id = id + 1;
			}
		});
		Applications.application.organizationNames.push({"id":id,"name":""});
		$scope.form.applicationDetail.$setDirty();
	}

	$scope.setDirty = function(){
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
				Applications.showMessage('danger', 'Operation failed - Status code: ' + response.data.status + " - " +  response.data.message);
				
			})
		}

	}

});