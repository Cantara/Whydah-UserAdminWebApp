

(function () {
   
   UseradminApp.directive('formDrctv', function() {
	   	  return {
	   		  
	   		  link: function(scope, el, attrs) {
	   			  
	   			
	   		   
	  		    scope.$watch(attrs.name + '.$error', function(newVal, oldVal) {
	  			      
	  		    }, true);
	   			  
	   		  }
	   	  }
		});


}());