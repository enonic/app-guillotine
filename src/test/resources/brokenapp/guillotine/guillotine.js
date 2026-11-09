// Extensions of an application whose validation fails AFTER a valid category has already been extracted.
// Nothing from this file may reach the schema.
exports.extensions = function (graphQL) {
    return {
        types: {
            // Same name as a type of the healthy application: would clobber it if partially extracted
            GoogleBooks: {
                description: 'Clobbered Google Books',
                fields: {
                    clobbered: {
                        type: graphQL.GraphQLString,
                    }
                }
            },
            // Depends on an enum of this application that never gets extracted
            BrokenAppType: {
                description: 'Broken App Type',
                fields: {
                    kind: {
                        type: graphQL.reference('BrokenAppEnum'),
                    }
                }
            }
        },
        // Processed after `types`: fails validation and must discard everything above
        enums: 'not an object',
    };
};
