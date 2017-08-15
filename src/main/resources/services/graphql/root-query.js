var graphQlLib = require('/lib/graphql');
var namingLib = require('/lib/headless-cms/naming');
var contentApiLib = require('./content-api');

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