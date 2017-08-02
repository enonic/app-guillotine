var graphQlLib = require('/lib/graphql');
var graphQlContentTypesLib = require('./graphql-content-types');
var graphQlContentApiLib = require('./graphql-content-api');


var createTypedApiTypeParams = {
    name: 'Types',
    description: 'Typed contents API',
    fields: {}
};
graphQlContentTypesLib.addContentTypesAsFields(createTypedApiTypeParams);
var typedApiType = graphQlLib.createObjectType(createTypedApiTypeParams);

exports.rootQueryType = graphQlLib.createObjectType({
    name: 'Query',
    fields: {
        types: {
            type: typedApiType,
            resolve: function () {
                return {};
            }
        },
        contents: {
            type: graphQlContentApiLib.contentApiType,
            resolve: function () {
                return {};
            }
        }
    }
});