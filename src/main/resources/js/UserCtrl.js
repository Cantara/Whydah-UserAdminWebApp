UseradminApp.controller('UserCtrl', function($scope, $http, $routeParams, Users, Applications) {

  $scope.session.activeTab = 'user';

  $scope.users = Users;
  $scope.applications = Applications;

  $scope.form = {};

  $scope.orderByColumn = 'username';
  $scope.orderReverse = false;

  $scope.addRoleForMultiple = false;

  var noUsersSelectedMessage = 'Please select a user first!';
  Users.requiredMessage = noUsersSelectedMessage;

  $scope.$watch('users.selected', function(){
    Users.requiredMessage = (Users.selected) ? '' : noUsersSelectedMessage;
  });

  $scope.searchUsers = function() {
    Users.search($scope.searchQuery);
  }

  $scope.clearAllApps = function() {
    console.log('Clear all');
    angular.forEach( Users.applications, function(el, index) {
      el.isSelected = false;
    });
  }

  $scope.getUserByUsername = function(username, callback) {
    console.log('Getting user by username:', username);
    $http({
      method: 'GET',
      url: myHostJsonUsers+username
    }).success(function(data){
      callback(data);
    });
  }

  $scope.activateUserDetail = function(username) {
    console.log('Activating user detail...', username);
    Users.get(username, function(){
      Users.getRolesForCurrentUser( function(){
        $scope.form.userDetail.$setPristine();
        $('#userdetail').modal('show');
      });
    });
  }

  $scope.newUserDetail = function() {
    Users.user = {isNew: true};
    Users.userRoles = {};
    $scope.form.userDetail.$setPristine();
    $('#userdetail').modal('show');
  }

  $scope.addRoleForUsers = function() {
    $scope.addRoleForMultiple = true;
    $('#addrole').modal('show');
  }

  $scope.addRoleForCurrentUser = function() {
    $scope.addRoleForMultiple = false;
    $('#addrole').modal('show');
  }

  $scope.resetPasswordForUsers = function() {
    var selectedUsernames = Users.getSelectedUsernames();
    var selectedUsers = Users.getSelectedUsers();
    if(window.confirm('Are you sure you want to reset password for users: '+selectedUsernames.join(', ')+'?')) {
        console.log('Resetting passwords.');
        for(var i=0; i<selectedUsers.length; i++) {
            Users.resetPassword(selectedUsers[i]);
        }
    }
  }

  $scope.changeOrder = function(orderByColumn) {
    $scope.orderByColumn = orderByColumn;
    $scope.orderReverse = !$scope.orderReverse;
  }

  $scope.textWrap = function(text, wrapOn){
    if(text.length < wrapOn){
      return text;
    }
    return text.substr(0, wrapOn) +"...";
  }
  function init() {
    Users.search();
    Applications.search();
    // Don't hide application-filter menu when clicking an option
    $('.dropdown-menu').click(function(e) {
      e.stopPropagation();
    });
  }

  if(Users.list.length<1) {
    init();
  }

});