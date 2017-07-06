var graphQlLib = require('/lib/graphql');
var graphQlRootQueryLib = require('./graphql-root-query');

exports.schema = graphQlLib.createSchema({
    query: graphQlRootQueryLib.rootQueryType
});