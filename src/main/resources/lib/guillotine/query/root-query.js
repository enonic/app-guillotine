const graphQlLib = require('/lib/guillotine/graphql');
const contentApiLib = require('/lib/guillotine/query/content-api');

function createRootQueryType(context) {
    return graphQlLib.createObjectType(context, {
        name: context.uniqueName('Query'),
        fields: {
            guillotine: {
                type: contentApiLib.createContentApiType(context),
                args: {
                    searchTarget: context.types.searchTargetInputType,
                },
                resolve: function (env) {
                    return {
                        repository: `com.enonic.cms.${env.args.searchTarget ? env.args.searchTarget.project : 'default'}`,
                        branch: env.args.searchTarget ? env.args.searchTarget.branch : 'draft',
                    };
                }
            }
        }
    });
}

exports.createRootQueryType = createRootQueryType;
