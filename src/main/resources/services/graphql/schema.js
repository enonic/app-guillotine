var graphQlLib = require('/lib/graphql');
var graphQlRootQueryLib = require('./root-query');

exports.schema = graphQlLib.createSchema({
    query: graphQlRootQueryLib.rootQueryType
});