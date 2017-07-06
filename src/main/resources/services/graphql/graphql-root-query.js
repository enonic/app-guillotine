var graphQlLib = require('/lib/graphql');

var createQueryTypeParams = {
    name: 'Query',
    fields: {
        dummy: {
            type: graphQlLib.GraphQLString,
            args: {
                id: graphQlLib.GraphQLID
            },
            resolve: function (env) {
                return 'dummyText';
            }
        }
    }
};

exports.rootQueryType = graphQlLib.createObjectType(createQueryTypeParams);