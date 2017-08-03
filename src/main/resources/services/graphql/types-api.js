var graphQlLib = require('/lib/graphql');
var contentTypesLib = require('./content-types');

var typesApiTypeParams = {
    name: 'TypesApi',
    description: 'Typed Content API',
    fields: {}
};
contentTypesLib.addContentTypesAsFields(typesApiTypeParams);
exports.typesApiType = graphQlLib.createObjectType(typesApiTypeParams);