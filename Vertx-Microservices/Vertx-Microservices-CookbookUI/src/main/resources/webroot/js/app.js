angular.module('CrudApp', []).config(['$routeProvider', function ($routeProvider) {
    $routeProvider.
        when('/', {templateUrl: './tpl/lists.html', controller: ListCtrl}).
        when('/add-book', {templateUrl: './tpl/add-new.html', controller: AddCtrl}).
        when('/add-recipe/:bookId', {templateUrl: './tpl/add-new-recipe.html', controller: AddRecipeCtrl}).
        when('/edit/:id', {templateUrl: './tpl/edit.html', controller: EditCtrl}).
        when('/edit/:bookId/recipe/:id', {templateUrl: './tpl/edit-recipe.html', controller: EditRecipeCtrl}).
        otherwise({redirectTo: '/'});
}]);

function ListCtrl($scope, $http) {
    $http.get('/cookbook-service').success(function (data) {
        $scope.books = data;
    });
}

function AddCtrl($scope, $http, $location) {
    $scope.master = {};
    $scope.activePath = null;

    $scope.add_new = function (book, AddNewForm) {

        $http.post('/cookbook-service/', book).success(function () {
            $scope.reset();
            $scope.activePath = $location.path('/');
        });

        $scope.reset = function () {
            $scope.user = angular.copy($scope.master);
        };

        $scope.reset();

    };
}

function AddRecipeCtrl($scope, $http, $location, $routeParams) {
	var bookId = $routeParams.bookId;
    $scope.master = {};
    $scope.activePath = null;
    $scope.recipe = {};
    $scope.recipe.bookId = bookId;

    $scope.add_new = function (recipe, AddNewForm) {

        $http.post('/cookbook-service/'+ bookId + "/recipe", recipe).success(function (data) {
            $scope.reset();
            $scope.activePath = $location.path('/edit/'+bookId);
        });

        $scope.reset = function () {
            $scope.recipe = angular.copy($scope.master);
        };

        $scope.reset();

    };
}

function EditCtrl($scope, $http, $location, $routeParams) {
    var id = $routeParams.id;
    $scope.activePath = null;

    $http.get('/cookbook-service/' + id).success(function (data) {
        $scope.book = data;
    });

    $scope.update = function (book) {
        $http.put('/cookbook-service/' + id, book).success(function (data) {
            $scope.book = data;
            $scope.activePath = $location.path('/');
        });
    };

    $scope.delete = function (book) {
        var deleteBook = confirm('Are you absolutely sure you want to delete ?');
        if (deleteBook) {
            $http.delete('/cookbook-service/' + id)
                .success(function(data, status, headers, config) {
                    $scope.activePath = $location.path('/');
                }).
                error(function(data, status, headers, config) {
                    console.log("error");
                    // custom handle error
                });
        }
    };
    
    $scope.delete_recipe = function(recipe) {
    	var deleteRecipe = confirm('Are you absolutely sure you want to delete this recipe?');
        if (deleteRecipe) {
            $http.delete('/cookbook-service/' + id + "/recipe/" + recipe.id)
                .success(function(data, status, headers, config) {
                	$scope.activePath = $location.path('/edit/'+recipe.bookId+'?reload=1');
                }).
                error(function(data, status, headers, config) {
                    console.log("error");
                    // custom handle error
                });
        }
    };
}

function EditRecipeCtrl($scope, $http, $location, $routeParams) {
    var id = $routeParams.id;
    var bookId = $routeParams.bookId;
    $scope.activePath = null;

    $http.get('/cookbook-service/' + bookId + "/recipe/" + id).success(function (data) {
        $scope.recipe = data;
    });

    $scope.update = function (recipe) {
        $http.put('/cookbook-service/' + bookId + "/recipe/" + id, recipe).success(function (data) {
            $scope.recipe = data;
            $scope.activePath = $location.path('/edit/:bookId');
        });
    };

    $scope.delete = function (recipe) {
        var deleteUser = confirm('Are you absolutely sure you want to delete ?');
        if (deleteUser) {
            $http.delete('/cookbook-service/' + bookId + "/recipe/" + id)
                .success(function(data, status, headers, config) {
                    $scope.activePath = $location.path('/');
                }).
                error(function(data, status, headers, config) {
                    console.log("error");
                    // custom handle error
                });
        }
    };
}