var graphQlLib = require('/lib/graphql');
var contentTypesLib = require('./content-types');

exports.createTypesApiType = function() {
    var typesApiTypeParams = {
        name: 'TypesApi',
        description: 'Typed Content API',
        fields: {}
    };
    contentTypesLib.addContentTypesAsFields(typesApiTypeParams);
    return graphQlLib.createObjectType(typesApiTypeParams);
}