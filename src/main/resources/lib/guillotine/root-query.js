var graphQlLib = require('/lib/graphql');

var contentApiLib = require('./content-api');
var namingLib = require('./naming');

exports.createRootQueryType = function () {
    return graphQlLib.createObjectType({
        name: namingLib.uniqueName('Query'),
        fields: {
            content: {
                type: contentApiLib.createContentApiType(),
                resolve: function () {
                    return {};
                }
            }
        }
    });
};