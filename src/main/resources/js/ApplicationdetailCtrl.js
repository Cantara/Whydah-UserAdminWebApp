UseradminApp.controller('ApplicationdetailCtrl', function ($scope,Applications, application, items) {

    $scope.applicationProperties = [
        {value: 'id',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
        {value: 'name',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
        {value: 'defaultOrganizationName',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
        {value: 'defaultRoleName',    minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
        {value: 'applicationUrl',     minLength: 2, maxLength: 64, required: true, type: 'text', validationMsg:'Must be between 2-64 characters long.'},
        {value: 'description',    required: false, type: 'text'},
        {value: 'fullTokenApplication',    required: false, type: 'text'},
        {value: 'logoUrl',    required: false, type: 'text'},
        {value: 'applicationJson', required: false, type: 'json'}
    ];
    $scope.dict = {
        en: {
            name: 'Application Name (*)',
            id: 'Application Id',
            defaultOrganizationName: 'Default Organization Name (*)',
            defaultRoleName: 'Default Role Name (*)',
            applicationUrl: 'URL to Application',
            description: 'Description of Application',
            fullTokenApplication: 'Whydah Admin application',
            logoUrl: 'URL to Application Logo',
            secret: 'Application Secret',
            applicationJson: 'Json override'
        }
    }
    $scope.application = applications.application;
    $scope.items = ['item1', 'item2', 'item3'];
    $scope.selected = {
        item: $scope.items[0]
    };

    $scope.ok = function () {
        //$uibModalInstance.close($scope.selected.item);
    };

    $scope.cancel = function () {
        //$uibModalInstance.dismiss('cancel');
    };
});