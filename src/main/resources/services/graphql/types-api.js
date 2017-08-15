var graphQlLib = require('/lib/graphql');
var namingLib = require('/lib/headless-cms/naming');
var contentTypesLib = require('./content-types');

exports.createTypesApiType = function() {
    var typesApiTypeParams = {
        name: namingLib.uniqueName('TypesApi'),
        description: 'Typed Content API',
        fields: {}
    };
    contentTypesLib.addContentTypesAsFields(typesApiTypeParams);
    return graphQlLib.createObjectType(typesApiTypeParams);
}