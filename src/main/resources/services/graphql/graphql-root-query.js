var graphQlLib = require('/lib/graphql');
var graphQlContentTypesLib = require('./graphql-content-types');
var graphQlContentApiLib = require('./graphql-content-api');


var createTypedApiTypeParams = {
    name: 'TypedApi',
    description: 'Typed contents API',
    fields: {}
};
graphQlContentTypesLib.addContentTypesAsFields(createTypedApiTypeParams);
var typedApiType = graphQlLib.createObjectType(createTypedApiTypeParams);

exports.rootQueryType = graphQlLib.createObjectType({
    name: 'Query',
    fields: {
        typedApi: {
            type: typedApiType,
            resolve: function () {
                return {};
            }
        },
        contentApi: {
            type: graphQlContentApiLib.contentApiType,
            resolve: function () {
                return {};
            }
        }
    }
});