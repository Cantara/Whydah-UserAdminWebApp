UseradminApp.directive('myTabs', function () {
        return {
            restrict: 'E',
            transclude: true,
            scope: {},
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
                title: '@'
            },
            link: function (scope, element, attrs, tabsCtrl) {
                tabsCtrl.addPane(scope);
            },
            templateUrl: 'template/directives/template_tabpane.html'
        };
    });