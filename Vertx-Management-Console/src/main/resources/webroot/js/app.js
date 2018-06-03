var module_route = angular.module('route', ['ngRoute']);
module_route.config(['$routeProvider', function ($routeProvider) {
    console.log("configuring routes");
    $routeProvider.
         when('/', {templateUrl: './tpl/overview.html', controller: 'OverviewCtrl'}).
         when('/metrics', {templateUrl: './tpl/metrics.html', controller: 'MetricsCtrl'}).
         when('/bus', {templateUrl: './tpl/bus.html', controller: 'BusCtrl'}).
         otherwise({redirectTo: '/'});
 }]);

var module_eventbus = angular.module('eventbus', ['knalli.angular-vertxbus']);
module_eventbus.config(function(vertxEventBusProvider, vertxEventBusServiceProvider) {
    console.log("configuring eventbus");
    vertxEventBusProvider
        .useDebug(true)
        .useUrlServer("http://" + location.host + "/managment-service/eventbus");
    vertxEventBusServiceProvider
        .useDebug(true)
        .authHandler('myCustomAuthHandler');
});

var app = angular.module('MetricsApp', ['ng', 'route', 'eventbus']);

/*
app.run(function ($rootScope, vertxEventBus, vertxEventBusService, $interval) {
	console.log("eventbus run method");
    $rootScope.sessionIsValid = false;

    $rootScope.moduleStats = {
      wrapper: {},
      service: {}
    };
    $interval(function () {
      try {
        $rootScope.moduleStats.wrapper.readyState = vertxEventBus.readyState();
        $rootScope.moduleStats.service.readyState = vertxEventBusService.readyState();
        $rootScope.moduleStats.service.getConnectionState = vertxEventBusService.getConnectionState();
        $rootScope.moduleStats.service.isEnabled = vertxEventBusService.isEnabled();
        $rootScope.moduleStats.service.isConnected = vertxEventBusService.isConnected();
        $rootScope.moduleStats.service.isAuthorized = vertxEventBusService.isAuthorized();
      } catch (e) {}
    }, 1000);
  });
  */

/*
app.filter('eventBusState', function () {
	console.log("adding filtering");
    var states = ['CONNECTING', 'OPEN', 'CLOSING', 'CLOSED'];
    return function (value) {
      return states[value] || value;
    };
  });

app.service('myCustomAuthHandler', function (vertxEventBus, $q) {
	var states = {
		      enabled: false
		    };
		    var service = function () {
		      console.log('authHandler invoked', states);
		      return $q(function (resolve, reject) {
		        if (states.enabled) {
		          vertxEventBus.applyDefaultHeaders({
		            token: 'VALID-123'
		          });
		          resolve();
		        } else {
		          reject();
		        }
		      });
		    };
		    service.start = function () {
		      states.enabled = true;
		    };
		    service.stop = function () {
		      states.enabled = false;
		      vertxEventBus.applyDefaultHeaders({});
		    };
		    return service;
		  });

app.controller('BusCtrl', function($scope, vertxEventBus, vertxEventBusService, myCustomAuthHandler){
	console.log("eventbus ctrl: registering listener");
	vertxEventBusService.on('metrics', function(err, message) {
	  console.log('Received a message: ', message);
	});
	
	var me = this;
    var holder = {};
	var sendCommand = function (type) {
	      vertxEventBusService.send('commands', {type: type})
	        .then(function (message) {
	          console.log('Command succeeded: ' + message.body.type)
	        }, function () {
	          console.log('Command failed')
	        });
	    };
	    me.sendPing = function () {
	      sendCommand('PING');
	    };
	    me.sendNonPing = function () {
	      sendCommand('INVALID');
	    };
	    me.enableAuthHandler = function () {
	      myCustomAuthHandler.start();
	    };
	    me.disableAuthHandler = function () {
	      myCustomAuthHandler.stop();
	    };
});
*/

app.controller('OverviewCtrl', function ($scope, $http) {
    $http.get('/managment-service/overview').then(function(response) {
        $scope.verticles = response.data;
    });
});

app.controller('MetricsCtrl', function ($scope, $http) {
    $http.get('/managment-service/metrics').then(function(response) {
    	console.log('Response: '+response.data);
        $scope.metrics = response.data;
    });

    $scope.expandSelected=function(metric){
        $scope.metrics.forEach(function(val){
          val.expanded=false;
        })

        $scope.metric=metric;

        metric.expanded=true;
        $http.get('/managment-service/metrics/'+metric.id).then(function (response) {
            $scope.metric.meta = response.data.meta;
        });
    }
});

app.controller('BusCtrl', function ($scope, $http) {
    $http.get('/managment-service/bus').then(function(response) {
        $scope.bus = response.data;
    });
});
