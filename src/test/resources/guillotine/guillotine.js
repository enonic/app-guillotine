exports.extensions = function (graphQL, guillotine) {
    return {
        inputTypes: {
            BooksInputFilter: {
                description: "Books Input Filter",
                fields: {
                    name: graphQL.GraphQLString,
                    startsWith: graphQL.GraphQLString,
                    endsWith: graphQL.GraphQLString,
                    contains: graphQL.GraphQLString,
                    customEnum: graphQL.reference('CustomEnum'),
                }
            }
        },
        enums: {
            CustomEnum: {
                description: "Custom Enum",
                values: {
                    a: "A",
                    b: "B",
                    c: "C"
                }
            }
        },
        interfaces: {
            CustomInterface: {
                description: "Custom Interface",
                fields: {
                    name: {
                        type: graphQL.GraphQLString,
                        args: {
                            filter: graphQL.reference('BooksInputFilter')
                        }
                    }
                },
            }
        },
        unions: {
            CustomUnion: {
                description: "Custom Union",
                types: [graphQL.reference('GoogleBooks'), graphQL.reference('GoogleBooksAuthor')],
            }
        },
        types: {
            ParentType: {
                description: 'Parent Type',
                fields: {
                    child: {
                        type: graphQL.reference('ChildType'),
                    }
                }
            },
            ChildType: {
                description: 'Child Type',
                fields: {
                    field: {
                        type: graphQL.GraphQLString,
                    }
                }
            },
            GoogleBooksAuthor: {
                description: 'Google Books Author',
                fields: {
                    name: {
                        type: graphQL.GraphQLString,
                        args: {
                            startsWith: graphQL.GraphQLString,
                        }
                    }
                }
            },
            GoogleBooks: {
                description: 'Google Books Type',
                fields: {
                    title: {
                        type: graphQL.GraphQLString,
                    },
                    description: {
                        type: graphQL.GraphQLString,
                    },
                    author: {
                        type: graphQL.reference('GoogleBooksAuthor'),
                        args: {
                            filter: graphQL.reference('BooksInputFilter'),
                        }
                    },
                }
            },
            CustomInterfaceImpl: {
                description: 'CustomInterface Implementation',
                interfaces: [graphQL.reference('CustomInterface')],
                fields: {
                    name: {
                        type: graphQL.GraphQLString,
                        args: {
                            filter: graphQL.reference('BooksInputFilter')
                        }
                    },
                    extraField: {
                        type: graphQL.GraphQLString,
                    }
                }
            }
        },
        creationCallbacks: {
            Query: function (params) {
                params.addFields({
                    customField: {
                        type: graphQL.nonNull(graphQL.list(graphQL.GraphQLString)),
                    },
                    googleBooks: {
                        type: graphQL.list(graphQL.reference('GoogleBooks')),
                    },
                    testUnion: {
                        type: graphQL.reference('CustomUnion'),
                    },
                    testInterface: {
                        type: graphQL.reference('CustomInterface')
                    },
                    testLocalContext: {
                        type: graphQL.reference('ParentType'),
                    },
                    invalidLocalContext: {
                        type: graphQL.reference('ParentType'),
                    },
                    testGetSiteUsingExtension: {
                        type: graphQL.reference('portal_Site'),
                        args: {
                            key: graphQL.nonNull(graphQL.GraphQLString),
                            project: graphQL.GraphQLString,
                            branch: graphQL.GraphQLString,
                        }
                    },
                    testGetContentUsingExtension: {
                        type: graphQL.reference('Content'),
                        args: {
                            key: graphQL.nonNull(graphQL.GraphQLString),
                            project: graphQL.GraphQLString,
                            branch: graphQL.GraphQLString,
                        }
                    },
                    testQueryUsingExtension: {
                        type: graphQL.list(graphQL.reference('Content')),
                        args: {
                            query: graphQL.nonNull(graphQL.reference('QueryDSLInput')),
                            project: graphQL.GraphQLString,
                            branch: graphQL.GraphQLString,
                        }
                    }
                });
            },
        },
        resolvers: {
            ParentType: {
                child: function (env) {
                    return graphQL.createDataFetcherResult({
                        data: __.toScriptValue({}),
                        localContext: {
                            b: 2,
                        },
                        parentLocalContext: env.localContext,
                    });
                }
            },
            ChildType: {
                field: function (env) {
                    return `a=${env.localContext.a} and b=${env.localContext.b}`;
                }
            },
            Query: {
                testLocalContext: function (env) {
                    return graphQL.createDataFetcherResult({
                        data: __.toScriptValue({}),
                        localContext: {
                            a: 1,
                        }
                    });
                },
                invalidLocalContext: function (env) {
                    return graphQL.createDataFetcherResult({
                        data: __.toScriptValue({}),
                        localContext: {
                            a: [1, 2, 3],
                        }
                    });
                },
                customField: function (env) {
                    return ["Value 1", "Value 2"];
                },
                googleBooks: function (env) {
                    return [{
                        title: "Title 1",
                        description: "Description 1",
                        author: {
                            name: "Author 1"
                        }
                    }, {
                        title: "Title 2",
                        description: "Description 2",
                        author: {
                            name: "Author 2"
                        }
                    }]
                },
                testUnion: function (env) {
                    return {
                        title: 'Title'
                    }
                },
                testInterface: function (env) {
                    return {
                        name: "First Name",
                        extraField: "Value",
                    }
                },
                testGetSiteUsingExtension: function (env) {
                    return guillotine.getSite(__.toScriptValue({
                        key: env.args.key,
                        project: env.args.project,
                        branch: env.args.branch,
                    }));
                },
                testGetContentUsingExtension: function (env) {
                    return guillotine.getContent(__.toScriptValue({
                        key: env.args.key,
                        project: env.args.project,
                        branch: env.args.branch,
                    }));
                },
                testQueryUsingExtension: function (env) {
                    const queryResult = guillotine.query(__.toScriptValue({
                        params: {
                            query: env.args.query
                        },
                        project: env.args.project,
                        branch: env.args.branch,
                    }));
                    return queryResult.hits;
                }
            },
            GoogleBooksAuthor: {
                name: function (env) {
                    return env.source.name;
                }
            },
            CustomInterface: {
                name: function (env) {
                    return 'No Name';
                }
            },
        },
        typeResolvers: {
            CustomInterface: function (obj) {
                return 'CustomInterfaceImpl';
            },
            CustomUnion: function (obj) {
                if (obj.title) {
                    return 'GoogleBooks';
                }
                return 'GoogleBooksAuthor';
            },
        },
    }
};
