UseradminApp.directive('myTabs', function () {
    return {
        restrict: 'E',
        transclude: true,
        scope:{},
        controller: ['$scope', function ($scope) {
            var panes = $scope.panes = [];

            $scope.select = function (pane) {

                angular.forEach(panes, function (pane) {
                    pane.selected = false;
                });
                pane.selected = true;
            };
            
          

            this.addPane = function (pane) {
                if (panes.length === 0) {
                    $scope.select(pane);
                }
                panes.push(pane);
            };
        }],
        templateUrl: 'template/directives/template_tab.html'
    };


})
.directive('myPane', function () {
    return {
        require: '^^myTabs',
        restrict: 'E',
        transclude: true,
        scope: {
            title: '@',
            ignore: '@'
        },
        link: function (scope, element, attrs, tabsCtrl) {
        	
            if(!scope.ignore){
                tabsCtrl.addPane(scope);
            }

        },
        templateUrl: 'template/directives/template_tabpane.html'
    };
});