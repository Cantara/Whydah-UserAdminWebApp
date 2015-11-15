/**
 * Created by baardl on 15.11.15.
 */
UseradminApp.controller('ApplicationdetailCtrl', function ($scope, $uibModalInstance, application, items) {

    $scope.application = application;
    $scope.items = ['item1', 'item2', 'item3'];
    $scope.selected = {
        item: $scope.items[0]
    };

    $scope.ok = function () {
        $uibModalInstance.close($scope.selected.item);
    };

    $scope.cancel = function () {
        $uibModalInstance.dismiss('cancel');
    };
});