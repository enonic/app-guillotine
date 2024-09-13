exports.extensions = function (graphQL) {
    return {
        inputTypes: {
            BooksInputFilter: {
                description: 'Books Input Filter',
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
                description: 'Custom Enum',
                values: {
                    a: 'A',
                    b: 'B',
                    c: 'C'
                }
            }
        },
        interfaces: {
            CustomInterface: {
                description: 'Custom Interface',
                fields: {
                    name: {
                        type: graphQL.GraphQLString,
                        args: {
                            filter: graphQL.reference('BooksInputFilter')
                        }
                    },
                    description: {
                        type: graphQL.GraphQLString,
                    },
                },
            }
        },
        unions: {
            CustomUnion: {
                description: 'Custom Union',
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
            DefaultCustomInterfaceImpl: {
                description: 'DefaultCustomInterface Implementation',
                interfaces: [graphQL.reference('CustomInterface')],
                fields: {
                    name: {
                        type: graphQL.GraphQLString,
                        args: {
                            filter: graphQL.reference('BooksInputFilter')
                        }
                    },
                    description: {
                        type: graphQL.GraphQLString,
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
                    description: {
                        type: graphQL.GraphQLString,
                    },
                    extraField: {
                        type: graphQL.GraphQLString,
                    },
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
                        type: graphQL.list(graphQL.reference('CustomInterface')),
                    },
                    testLocalContext: {
                        type: graphQL.reference('ParentType'),
                    },
                    invalidLocalContext: {
                        type: graphQL.reference('ParentType'),
                    },
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
                    return ['Value 1', 'Value 2'];
                },
                googleBooks: function (env) {
                    return [{
                        title: 'Title 1',
                        description: 'Description 1',
                        author: {
                            name: 'Author 1'
                        }
                    }, {
                        title: 'Title 2',
                        description: 'Description 2',
                        author: {
                            name: 'Author 2'
                        }
                    }];
                },
                testUnion: function (env) {
                    return {
                        title: 'Title'
                    };
                },
                testInterface: function (env) {
                    return [
                        {
                            name: 'Name 1',
                            extraField: 'Value',
                            description: 'Description'
                        },
                        {
                            name: 'Name 2',
                            description: 'Brief Description'
                        }
                    ];
                }
            },
            GoogleBooksAuthor: {
                name: function (env) {
                    return env.source.name;
                }
            },
            CustomInterface: {
                description: function (env) {
                    return env.source.description + ' - CustomInterface';
                },
            },
            CustomInterfaceImpl: {
                description: function (env) {
                    return env.source.description + ' - CustomInterfaceImpl';
                }
            }
        },
        typeResolvers: {
            CustomInterface: function (obj) {
                if (obj.extraField) {
                    return 'CustomInterfaceImpl';
                }
                return 'DefaultCustomInterfaceImpl';
            },
            CustomUnion: function (obj) {
                if (obj.title) {
                    return 'GoogleBooks';
                }
                return 'GoogleBooksAuthor';
            },
        },
    };
};
