var mod = angular.module("SuperLab.Directives", []);

mod.run(function($rootScope) {
    $rootScope.addDetailScope = function (item) {
        $rootScope.details = $rootScope.details || [];
        $rootScope.details.push(item);
    };
    $rootScope.isEditing = function() {
        return _.some($rootScope.details, function(item) { return item.isEditing; });
    };
});

var protSimpleItem = {
        "showBar": function() {
            if (!this.barVisible && !this.$root.isEditing()) {
                this.barVisible = true;
                this.iconsSpan.show();
            }
        },
        "hideBar": function() {
            if (!this.$root.isEditing()) {
                this.barVisible = this.$root.isEditing();
                this.iconsSpan.hide();
            }
        },
        "initEdit": function(event) {
            if (!this.$root.isEditing() && !this.disableEdit) {
                this.save = _.clone(this.data);
                $("div.simple-data").css('cursor', 'default');
                if ((typeof(this.data.__new__) != "undefined") && (this.newDiv.length > 0) && (this.data.__new__ == true)) 
                    this.newDiv.show();
                else 
                    this.editDiv.show();
                this.viewDiv.hide();
                this.iconsSpan.removeClass('disabled');
                this.isEditing = true;
            }
        },
        "commitEdit": function(event, action) {
            if (this.isEditing) {
                var self = this;

                $("div.simple-data").css('cursor', 'pointer');
                this.newDiv.hide();
                this.editDiv.hide();
                this.viewDiv.show();
                this.iconsSpan.addClass('disabled');
                this.isEditing = false;
                _.defer(function() { self.hideBar(); });

                if (action['delete']) {
                    this.deleteAction(this.data);
                }
                else {
                    if (action['abort']) {
                        if (this.data.__new__)
                            this.deleteAction(this.data);
                        else
                            _.extend(this.data, this.save);
                    }
                    else {
                        var updated = false;
                        if (this.saveFunc) this.saveFunc(this.data);

                        for (var prop in this.save) {
                            if (this.save[prop] != this.data[prop]) {
                                updated = true;
                                break;
                            }
                        }

                        if (!updated) {                        
                            if (typeof(this.data.__new__) == "undefined") {
                                this.deleteAction(this.data);
                            }
                        }
                        else {
                            if (typeof(this.data.__new__) != "undefined") this.data.__new__ = false;
                        }
                    }
                }
                event.stopPropagation();                
            }
        }
    };

var simpleItem = function () {
    return {
        replace: true,
        transclude: true,
        scope: { data: "=simpleItem", 
                 deleteAction: "=deleteAction", 
                 disableEdit: "=disableEdit",
                 saveFunc: "=saveItem" },
        controller: function ($scope, $element, $attrs, $rootScope) {
            _.extend($scope, protSimpleItem);
            $rootScope.addDetailScope($scope);
        },
        link: function postLink($scope, $element) {
            // salvando elementos
            $scope.editDiv = $element.find("div.edit");
            $scope.viewDiv = $element.find("div.view");
            $scope.newDiv = $element.find("div.new");
            $scope.iconsSpan = $element.find("span.icons");

            // Estabelecendo estado inicial
            $scope.newDiv.hide();
            $scope.editDiv.hide();
            $scope.viewDiv.show();
            $scope.iconsSpan.hide();
            $scope.iconsSpan.addClass('disabled');
            $scope.barVisible = false;
            $scope.isEditing = false;

            // Acrescentando handler de ENTER = SAVE para as entradas de texto
            $scope.editDiv.find("input").keypress(
                function (e) {
                    if (e.keyCode == 13) $element.find("a.glyphicon.save").click();
                });
            // E ESC = CANCEL para todos
            var dismiss = function (e) {
                if (e.keyCode == 27) $element.find("a.glyphicon.cancel").click();
            };

            $scope.editDiv.find("input").keyup(dismiss);
            $scope.editDiv.find("select").keyup(dismiss);

            if ($scope.data.__new__) {
                _.defer(function () {
                    $scope.showBar();
                    $element.click();
                    _.defer(function () { $element.find("select").first().focus(); })
                });
            }
        },
        template: "HTML code"
    };
};

var emptyOrNull = function(v) {
    return typeof(v) == "undefined" || v == null || v == "";
};

var lookupCep = function() {
    return {
        replace: false,
        scope: { item: "=lookupCep" },
        controller: function ($scope, $element, $attrs, $rootScope, $http) {
            $element.on("blur", function(e) {
                if ($scope.item.CEP != "") {
                    $http({
                        method:"GET",
                        url: $Url.resolve("~/svc/tabelas/cep/" + $element.val())
                    }).success(function(data) {
                        if (!emptyOrNull(data.Logradouro) && emptyOrNull($scope.item.Logradouro)) $scope.item.Logradouro = data.Logradouro;
                        if (!emptyOrNull(data.Bairro) && emptyOrNull($scope.item.Bairro)) $scope.item.Bairro = data.Bairro;
                        if (!emptyOrNull(data.Cidade) && emptyOrNull($scope.item.Cidade)) $scope.item.Cidade = data.Cidade;
                        if (!emptyOrNull(data.UF) && emptyOrNull($scope.item.UF)) $scope.item.UF = data.UF;
                    });
                }
            });
        }
    };
};

mod.directive('simpleItem', simpleItem)
   .directive('simpleFormat', simpleFormat)
   .directive('lookupCep', lookupCep);