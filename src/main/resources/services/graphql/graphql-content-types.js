var graphQlLib = require('/lib/graphql');
var contentLib = require('/lib/xp/content');

exports.addContentTypesAsFields = function (createObjectTypeParams) {
    contentLib.getTypes().forEach(function (contentType) {
        var contentTypeName = getContentTypeLocalName(contentType);
        var contentTypeObjectType = generateContentTypeObjectType(contentType);
        createObjectTypeParams.fields['get_' + contentTypeName] = {
            type: contentTypeObjectType,
            args: {
                key: graphQlLib.nonNull(graphQlLib.GraphQLID)
            },
            resolve: function (env) {
                return contentLib.getContent(env.args.key);
            }
        };
        createObjectTypeParams.fields['getAll_' + contentTypeName] = {
            type: graphQlLib.list(contentTypeObjectType),
            args: {
                offset: graphQlLib.GraphQLInt,
                first: graphQlLib.GraphQLInt
            },
            resolve: function (env) {
                var offset = env.args.offset;
                var first = env.args.first;
                return contentLib.query({
                    query: 'type = \'' + contentType.name + '\'',
                    start: offset,
                    count: first
                }).hits;
            }
        };
    });
};

function getContentTypeLocalName(contentType) {
    var localName = contentType.name.substr(contentType.name.indexOf(':') + 1);
    return sanitizeText(localName);
}

function generateContentTypeObjectType(contentType) {
    log.info('Content type: ' + JSON.stringify(contentType, null, 2));
    var contentTypeDisplayName = sanitizeText(contentType.displayName);

    var createContentTypeTypeParams = {
        name: contentTypeDisplayName,
        description: contentType.displayName,
        fields: {}
    };
    addContentTypeFields(createContentTypeTypeParams, contentType);
    return graphQlLib.createObjectType(createContentTypeTypeParams);
}


function addContentTypeFields(createContentTypeTypeParams, contentType) {
    var fields = createContentTypeTypeParams.fields;
    fields._id = {
        type: graphQlLib.nonNull(graphQlLib.GraphQLID),
        resolve: function (env) {
            return env.source._id;
        }
    };
    fields._name = {
        type: graphQlLib.nonNull(graphQlLib.GraphQLString),
        resolve: function (env) {
            return env.source._name;
        }
    };
    fields._path = {
        type: graphQlLib.nonNull(graphQlLib.GraphQLString),
        resolve: function (env) {
            return env.source._path;
        }
    };
    fields.creator = {
        type: graphQlLib.GraphQLString,
        resolve: function (env) {
            return env.source.creator;
        }
    };
    fields.modifier = {
        type: graphQlLib.GraphQLString,
        resolve: function (env) {
            return env.source.modifier;
        }
    };
    fields.createdTime = {
        type: graphQlLib.GraphQLString,
        resolve: function (env) {
            return env.source.createdTime;
        }
    };
    fields.modifiedTime = {
        type: graphQlLib.GraphQLString,
        resolve: function (env) {
            return env.source.modifiedTime;
        }
    };
    fields.type = {
        type: graphQlLib.GraphQLString,
        resolve: function (env) {
            return env.source.type;
        }
    };
    fields.displayName = {
        type: graphQlLib.GraphQLString,
        resolve: function (env) {
            return env.source.displayName;
        }
    };
    fields.hasChildren = {
        type: graphQlLib.GraphQLBoolean,
        resolve: function (env) {
            return env.source.hasChildren;
        }
    };
    fields.modifiedTime = {
        type: graphQlLib.GraphQLString,
        resolve: function (env) {
            return env.source.modifiedTime;
        }
    };
    fields.valid = {
        type: graphQlLib.GraphQLBoolean,
        resolve: function (env) {
            return env.source.valid;
        }
    };
    if (contentType.form.length > 0) {
        fields.data = {
            type: generateContentTypeDataObjectType(contentType),
            resolve: function (env) {
                return env.source.data;
            }
        };
    }
    fields.x = {
        type: graphQlLib.GraphQLString,
        resolve: function (env) {
            return JSON.stringify(env.source.x); //TODO
        }
    };
    fields.page = {
        type: graphQlLib.GraphQLString,
        resolve: function (env) {
            return JSON.stringify(env.source.page); //TODO
        }
    };
    fields.attachments = {
        type: graphQlLib.GraphQLString,
        resolve: function (env) {
            return JSON.stringify(env.source.attachments); //TODO
        }
    };
    fields.publish = {
        type: graphQlLib.GraphQLString,
        resolve: function (env) {
            return JSON.stringify(env.source.publish); //TODO
        }
    }
    //TODO Add missing fields
}

function generateContentTypeDataObjectType(contentType) {
    var createContentTypeDataTypeParams = {
        name: sanitizeText(contentType.displayName) + '_Data',
        description: contentType.displayName + ' data',
        fields: {}
    };
    contentType.form.forEach(function (formItem) {
        createContentTypeDataTypeParams.fields[sanitizeText(formItem.name)] = {
            type: graphQlLib.GraphQLString, //TODO
            resolve: function (env) {
                return env.source[formItem.name];
            }
        }
    });
    return graphQlLib.createObjectType(createContentTypeDataTypeParams);
}

function sanitizeText(text) {
    return text.replace(/([^0-9A-Za-z])+/g, '_');
}  


