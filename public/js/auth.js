(function() {
    var auth = angular.module('auth', []).run(function($rootScope) {
        $rootScope.displayLoginForm = true;
    });

    auth.controller('LoginController', function($scope, $rootScope) {
        $scope.user = {};
        
        $scope.showSignUp = function() {
            $rootScope.displayLoginForm = false;
        };
        
        $scope.login = function() {
        
        };
    });

    auth.controller('SignUpController', function($scope, $rootScope) {
        $scope.user = {};

        $scope.showLogin = function() {
            $rootScope.displayLoginForm = true;
        };

        $scope.signup = function() {
        
        };
    });
})();