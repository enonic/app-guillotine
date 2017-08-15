var graphQlLib = require('/lib/graphql');
var contentApiLib = require('./content-api');

exports.createRootQueryType = function () {
    return graphQlLib.createObjectType({
        name: 'Query',
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