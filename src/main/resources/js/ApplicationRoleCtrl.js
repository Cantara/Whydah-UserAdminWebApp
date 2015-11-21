app.controller('EditableRowCtrl', function($scope, $filter, $http) {
    $scope.roles = [
        {id: 1, name: 'awesome user1', status: 2, group: 4, groupName: 'admin'},
        {id: 2, name: 'awesome user2', status: undefined, group: 3, groupName: 'vip'},
        {id: 3, name: 'awesome user3', status: 2, group: null}
    ];
    $scope.checkRoleName = function(data, id) {
        if (id === 2 && data !== 'awesome') {
            return "Username 2 should be `awesome`";
        }
    };

    $scope.saveRole = function(data, id) {
        //$scope.user not updated yet
        angular.extend(data, {id: id});
        return $http.post('/saveUser', data);
    };

    // remove user
    $scope.removeRole = function(index) {
        $scope.users.splice(index, 1);
    };

    // add user
    $scope.addRole = function() {
        $scope.inserted = {
            id: $scope.users.length+1,
            name: '',
            status: null,
            group: null
        };
        $scope.users.push($scope.inserted);
    };
});