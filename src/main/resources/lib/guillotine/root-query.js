var graphQlLib = require('/lib/graphql');

var guillotineLib = require('./guillotine');

exports.createRootQueryType = function (context) {
    return graphQlLib.createObjectType({
        name: context.uniqueName('Query'),
        fields: {
            guillotine: {
                type: guillotineLib.createContentApi(context),
                resolve: function () {
                    return {};
                }
            }
        }
    });
};