// Schema extensions for the e2e checks of extension fields: whose context an app's resolver runs in over MCP and over HTTP.
const authLib = require('/lib/xp/auth');
const contentLib = require('/lib/xp/content');
const contextLib = require('/lib/xp/context');

const ARTICLE = 'com.example.mcpdataset:article';

function articleCount() {
    return contentLib.query({count: 0, contentTypes: [ARTICLE]}).total;
}

exports.extensions = function (graphQL) {
    return {
        creationCallbacks: {
            com_example_mcpdataset_Article: function (params) {
                params.addFields({
                    viewer: {type: graphQL.GraphQLString}
                });
            },
            HeadlessCms: function (params) {
                params.addFields({
                    readableArticleCount: {type: graphQL.GraphQLInt},
                    // elevates on purpose: what an app's own elevation looks like through MCP
                    articleCountAsAdmin: {type: graphQL.GraphQLInt}
                });
            }
        },
        resolvers: {
            com_example_mcpdataset_Article: {
                viewer: function () {
                    const user = authLib.getUser();
                    return user ? user.key : 'anonymous';
                }
            },
            HeadlessCms: {
                readableArticleCount: function () {
                    return articleCount();
                },
                articleCountAsAdmin: function () {
                    return contextLib.run({principals: ['role:system.admin']}, articleCount);
                }
            }
        }
    };
};
