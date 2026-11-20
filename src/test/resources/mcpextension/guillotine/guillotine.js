exports.extensions = function (graphQL) {
    return {
        creationCallbacks: {
            com_example_mcpdataset_Article: function (params) {
                params.addFields({
                    wordCount: {
                        type: graphQL.GraphQLInt
                    }
                });
            },
            HeadlessCms: function (params) {
                params.addFields({
                    latestArticles: {
                        type: graphQL.list(graphQL.reference('com_example_mcpdataset_Article'))
                    }
                });
            }
        },
        resolvers: {
            com_example_mcpdataset_Article: {
                wordCount: function () {
                    return 42;
                }
            },
            HeadlessCms: {
                latestArticles: function () {
                    return [];
                }
            }
        }
    };
};
