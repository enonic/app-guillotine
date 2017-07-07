var graphQlLib = require('/lib/graphql');

exports.publishInfoType = graphQlLib.createObjectType({
    name: 'PublishInfo',
    description: 'Publish information.',
    fields: {
        from: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.from;
            }
        },
        to: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.to;
            }
        },
        first: {
            type:graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.first;
            }
        }
    }
});