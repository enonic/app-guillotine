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

exports.attachmentType = graphQlLib.createObjectType({
    name: 'Attachment',
    description: 'Attachment.',
    fields: {
        name: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.name;
            }
        },
        label: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.label;
            }
        },
        size: {
            type:graphQlLib.GraphQLInt,
            resolve: function (env) {
                return env.source.size;
            }
        },
        mimeType: {
            type:graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.mimeType;
            }
        }
    }
});