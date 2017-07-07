var graphQlLib = require('/lib/graphql');
var graphQlContentTypesLib = require('./graphql-content-types');

var createQueryTypeParams = {
    name: 'Query',
    fields: {
    }    
};
graphQlContentTypesLib.addContentTypesAsFields(createQueryTypeParams);
exports.rootQueryType = graphQlLib.createObjectType(createQueryTypeParams);