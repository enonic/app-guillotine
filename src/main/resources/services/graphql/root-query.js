var graphQlLib = require('/lib/graphql');
var contentApiLib = require('./content-api');

exports.rootQueryType = graphQlLib.createObjectType({
    name: 'Query',
    fields: {
        content: {
            type: contentApiLib.contentApiType,
            resolve: function () {
                return {};
            }
        }
    }
});