exports.extensions = function (graphQL) {
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
            GoogleBooksAuthor: {
                description: 'Google Books Author',
                fields: {
                    name: {
                        type: graphQL.GraphQLString,
                        args: {
                            first: graphQL.GraphQLString,
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
                    }
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
                        args: {
                            start: graphQL.GraphQLString
                        },
                    },
                    googleBooks: {
                        type: graphQL.list(graphQL.reference('GoogleBooks')),
                    },
                    testUnion: {
                        type: graphQL.reference('CustomUnion'),
                    },
                    testInterface: {
                        type: graphQL.reference('CustomInterface')
                    }
                });
            },
        },
        resolvers: {
            Query: {
                customField: function (env) {
                    return ["Static value " + env.args.start];
                },
                googleBooks: function (env) {
                    return [{
                        title: "Java One",
                        description: "Description 1",
                        author: {
                            name: "Name 1 "
                        }
                    }, {
                        title: "Java Script",
                        description: "Description 2",
                        author: {
                            name: "Name 2 "
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
                }
            },
            GoogleBooksAuthor: {
                name: function (env) {
                    return env.source.name + ' ' + (env.args.first || '')
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
