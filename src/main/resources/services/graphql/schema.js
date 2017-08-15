var graphQlLib = require('/lib/graphql');
var graphQlRootQueryLib = require('./root-query');

exports.createSchema = function () {
    return graphQlLib.createSchema({
        query: graphQlRootQueryLib.createRootQueryType()
    })
};