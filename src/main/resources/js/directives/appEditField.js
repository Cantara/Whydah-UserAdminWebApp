  (function () {
    
	
    var controller = ['$scope', function ($scope) {
		
          function init() {
          
        	
        	    $scope.items = angular.copy($scope.appProperties);
        	    $scope.property = {value: 'name'};
             	angular.forEach($scope.items, function(item, index){
             	
					if(item.value === $scope.propertyName){
						$scope.property = item;
					}
				   
				});
          }

          init();
          
         
	
      }];
      
     
      var link = function(scope, element, attrs, ctrls){
    	  scope.form = ctrls[0];
          var ngModel = ctrls[1];
    	  
          
          
	      
	      ngModel.$render = function(){
	    	 
		      scope.value = ngModel.$viewValue;
		     
		  };
	      
		  scope.checkValidation = function(){
			 
			  if(!scope.form.$error){
				  return;
			  }
			  var errorList = new Object();
			  angular.forEach(scope.form.$error, function(value, key) {
			       
			   
			        if (value) {
			          
			          for (var i = 0; i < value.length; i++) {
			          
			            if(value[i].$dirty){
			              
			           
			  
			            var errFound = false;
			            var err = '';
			            angular.forEach(value[i].$error, function(errVal, errKey) {
			              if (!errFound && errVal) {
			                errFound = true;
			                err = errKey;
			                
			                  
			              
			              }
			            });
			  
			            if (errFound) {
			               console.log(value[i].$name + 'has error ' + err);
			               errorList[value[i].$name] = err;
			            }
			          
			          }
			          }
			          
			          

			        }



			      });
			  
			
			  
			  if (errorList[scope.property.value] != undefined) {
		          console.log(scope.property.value + "->" + errorList[scope.property.value]);
		          scope.form.$setError(scope.property.value, errorList[scope.property.value]);
		      } else {
		          console.log(scope.property.value + "-> OK NOW");
		          scope.form.$setError(scope.property.value, null);
		      }
			      
			
			  
		  }
	     
	      
		  if(scope.optionValue && scope.optionDisplay){
			  scope.expression = "o."+scope.optionValue +" as o."+scope.optionDisplay+" for o in options";
		  } else {
			  scope.expression = "o for o in options";
		  }
      };
      
    
    var directive  = function () {
        return {
        	require: ['^form', 'ngModel'],
            restrict: 'EA',
            scope: {
            	
                appProperties: '=',
                propertyName: '@',
                label: '@',
                filterPropertyName: '@',
                options:'=',
                optionValue:'@',
                optionDisplay:'@',
                optionSelectedField:'@'
                
            },
          
            templateUrl:"template/directives/appEditField.html",
            controller: controller,
            link: link
        };
    };
    
    

    UseradminApp.directive('appEditField', directive );

}());