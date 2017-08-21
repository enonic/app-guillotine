var graphQlLib = require('/lib/graphql');

var contentApiLib = require('./content-api');
var namingLib = require('./naming');

exports.createRootQueryType = function (context) {
    return graphQlLib.createObjectType({
        name: namingLib.uniqueName('Query'),
        fields: {
            content: {
                type: contentApiLib.createContentApiType(context),
                resolve: function () {
                    return {};
                }
            }
        }
    });
};