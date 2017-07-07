var graphQlLib = require('/lib/graphql');
var contentLib = require('/lib/xp/content');
var graphqlContentObjectTypesLib = require('./graphql-content-object-types');

exports.addContentTypesAsFields = function (createObjectTypeParams) {
    contentLib.getTypes().forEach(function (contentType) {
        var contentTypeName = getContentTypeLocalName(contentType);
        var contentTypeObjectType = generateContentTypeObjectType(contentType);
        createObjectTypeParams.fields['get' + contentTypeName] = {
            type: contentTypeObjectType,
            args: {
                key: graphQlLib.nonNull(graphQlLib.GraphQLID)
            },
            resolve: function (env) {
                return contentLib.getContent(env.args.key);
            }
        };
        createObjectTypeParams.fields['getAll' + contentTypeName] = {
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
    return generateCamelCase(localName, true);
}

function generateContentTypeObjectType(contentType) {
    log.info('Content type: ' + JSON.stringify(contentType, null, 2));
    var contentTypeDisplayName = generateCamelCase(contentType.displayName, true);

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
    fields.valid = {
        type: graphQlLib.GraphQLBoolean,
        resolve: function (env) {
            return env.source.valid;
        }
    };
    //if (contentType.form.length > 0) {
    //    fields.data = {
    //        type: generateContentTypeDataObjectType(contentType),
    //        resolve: function (env) {
    //            return env.source.data;
    //        }
    //    };
    //}
    
    fields.data = {
        type: graphQlLib.GraphQLString,
        resolve: function (env) {
            return JSON.stringify(env.source.data); //TODO
        }
    };
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
        type: graphQlLib.list(graphqlContentObjectTypesLib.attachmentType),
        resolve: function (env) {
            return Object.keys(env.source.attachments).map(function(key){
                return env.source.attachments[key];
            });
        }
    };
    fields.publish = {
        type: graphqlContentObjectTypesLib.publishInfoType,
        resolve: function (env) {
            return env.source.publish; //TODO
        }
    }
    //TODO Add missing fields
}

//function generateContentTypeDataObjectType(contentType) {
//    var createContentTypeDataTypeParams = {
//        name: generateCamelCase(contentType.displayName + '_Data', true),
//        description: contentType.displayName + ' data',
//        fields: {}
//    };
//    contentType.form.forEach(function (formItem) {
//        createContentTypeDataTypeParams.fields[generateCamelCase(formItem.name)] = {
//            type: graphQlLib.GraphQLString, //TODO
//            resolve: function (env) {
//                return env.source[formItem.name];
//            }
//        }
//    });
//    return graphQlLib.createObjectType(createContentTypeDataTypeParams);
//}

function generateCamelCase(text, upper) {
    var sanitizedText = sanitizeText(text);
    var camelCasedText = sanitizedText.replace(/_[0-9A-Za-z]/g, function (match, offset, string) {
        return match.charAt(1).toUpperCase();
    });
    var firstCharacter = upper ? camelCasedText.charAt(0).toUpperCase() : camelCasedText.charAt(0).toLowerCase();
    return firstCharacter + (camelCasedText.length > 1 ? camelCasedText.substr(1) : '');
}

function sanitizeText(text) {
    return text.replace(/([^0-9A-Za-z])+/g, '_');
}  


