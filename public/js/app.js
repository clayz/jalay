(function() {
    var app = angular.module('jalay', ['ngRoute'])
        .config(function($routeProvider) {
            $routeProvider
                .when('/', {
                    templateUrl : 'assets/pages/login.html',
                    controller  : 'LoginController'
                })
                .when('/login', {
                    templateUrl : 'assets/pages/login.html',
                    controller  : 'LoginController'
                })
                .when('/signup', {
                    templateUrl : 'assets/pages/signup.html',
                    controller  : 'SignUpController'
                })
                .when('/home', {
                    templateUrl : 'assets/pages/home.html',
                    controller  : 'HomeController'
                });
        }).config(function($httpProvider) {
            // change default HTTP post data format from json to normal form data
            $httpProvider.defaults.headers.post['Content-Type'] = 'application/x-www-form-urlencoded; charset=UTF-8';
            $httpProvider.defaults.transformRequest = function(data) {
                if (data === undefined) return data;
                return $.param(data);
            }
        });
    
    app.controller('LoginController', function($scope, $http, $location) {
        $scope.user = {};

        $scope.login = function() {
            if(!$scope.loginform.$valid) return;

            $http({
                method: 'POST',
                url: '/api/login/',
                data: $scope.user
            }).success(function (data, status, headers, config) {
                if (data.status == '000000') {
                    $location.path('/home');
                } else {
                    $scope.loginFailed = true;
                }
            });
        };
        
        $scope.signUp = function() {
            $location.path('/signup')        
        };
    });

    app.controller('SignUpController', function($scope, $http, $location) {
        $scope.user;
        
        $scope.setGender = function(gender) {
            $scope.user.gender = gender;
        };

        $scope.signup = function() {
            if(!$scope.signupform.$valid) return;
            
            $http({
                method: 'POST',
                url: '/api/register/',
                data: $scope.user
            }).success(function (data, status, headers, config) {
                if (data.status == '000000') {
                    $location.path('/home');
                } else {
                    $scope.registerFailed = true;
                }
            });
        };
        
        $scope.login = function() {
            $location.path('/login')
        };
    });

    app.controller('HomeController', function() {

    });
})();