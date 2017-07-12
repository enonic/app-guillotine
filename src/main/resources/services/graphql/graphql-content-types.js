var graphQlLib = require('/lib/graphql');
var contentLib = require('/lib/xp/content');
var utilLib = require('./util');
var graphqlContentObjectTypesLib = require('./graphql-content-object-types');

exports.addContentTypesAsFields = function (parentObjectTypeParams) {

    //For each content type
    contentLib.getTypes().
        //filter(function (type) {
        //    return type.name.indexOf(':fieldset') != -1
        //}).
        forEach(function (contentType) {
            var camelCaseContentTypeName = getCamelCaseContentTypeName(contentType);

            //Generates the object type for this content type
            var contentTypeObjectType = generateContentTypeObjectType(contentType);

            //Creates a root query field getXXX finding a content by key 
            parentObjectTypeParams.fields['get' + camelCaseContentTypeName] = {
                type: contentTypeObjectType,
                args: {
                    key: graphQlLib.nonNull(graphQlLib.GraphQLID)
                },
                resolve: function (env) {
                    var content = contentLib.getContent(env.args.key);
                    return content && content.type === contentType.name ? content : null;
                }
            };

            //Creates a root query field getXXXList finding contents
            parentObjectTypeParams.fields['get' + camelCaseContentTypeName + 'List'] = {
                type: graphQlLib.list(contentTypeObjectType),
                args: {
                    offset: graphQlLib.GraphQLInt,
                    first: graphQlLib.GraphQLInt
                },
                resolve: function (env) {
                    var offset = env.args.offset;
                    var first = env.args.first;
                    var contents = contentLib.query({
                        query: 'type = \'' + contentType.name + '\'',
                        start: offset,
                        count: first
                    }).hits;
                    log.info('contents:' + JSON.stringify(contents, null, 2));
                    return contents;
                }
            };
        });
};

function getCamelCaseContentTypeName(contentType) {
    var localName = contentType.name.substr(contentType.name.indexOf(':') + 1);
    return generateCamelCase(localName, true);
}

function generateContentTypeObjectType(contentType) {
    var createContentTypeTypeParams = {
        name: generateCamelCase(contentType.displayName, true),
        description: contentType.displayName,
        fields: {
            _id: {
                type: graphQlLib.nonNull(graphQlLib.GraphQLID),
                resolve: function (env) {
                    return env.source._id;
                }
            },
            _name: {
                type: graphQlLib.nonNull(graphQlLib.GraphQLString),
                resolve: function (env) {
                    return env.source._name;
                }
            },
            _path: {
                type: graphQlLib.nonNull(graphQlLib.GraphQLString),
                resolve: function (env) {
                    return env.source._path;
                }
            },
            creator: {
                type: graphqlContentObjectTypesLib.principalKeyType,
                resolve: function (env) {
                    return env.source.creator;
                }
            },
            modifier: {
                type: graphqlContentObjectTypesLib.principalKeyType,
                resolve: function (env) {
                    return env.source.modifier;
                }
            },
            createdTime: {
                type: graphQlLib.GraphQLString,
                resolve: function (env) {
                    return env.source.createdTime;
                }
            },
            modifiedTime: {
                type: graphQlLib.GraphQLString,
                resolve: function (env) {
                    return env.source.modifiedTime;
                }
            },
            owner: {
                type: graphqlContentObjectTypesLib.principalKeyType,
                resolve: function (env) {
                    return env.source.owner;
                }
            },
            type: {
                type: graphqlContentObjectTypesLib.contentTypeNameType,
                resolve: function (env) {
                    return env.source.type;
                }
            },
            displayName: {
                type: graphQlLib.GraphQLString,
                resolve: function (env) {
                    return env.source.displayName;
                }
            },
            hasChildren: {
                type: graphQlLib.GraphQLBoolean,
                resolve: function (env) {
                    return env.source.hasChildren;
                }
            },
            language: {
                type: graphQlLib.GraphQLString,
                resolve: function (env) {
                    return env.source.language;
                }
            },
            valid: {
                type: graphQlLib.GraphQLBoolean,
                resolve: function (env) {
                    return env.source.valid;
                }
            },
            data: getFormItems(contentType.form).length > 0 ? {
                type: generateContentDataObjectType(contentType),
                resolve: function (env) {
                    return env.source.data;
                }
            } : undefined,
            x: {
                type: graphQlLib.GraphQLString,
                resolve: function (env) {
                    return JSON.stringify(env.source.x); //TODO
                }
            },
            page: {
                type: graphqlContentObjectTypesLib.pageType,
                resolve: function (env) {
                    return env.source.page;
                }
            },
            attachments: {
                type: graphQlLib.list(graphqlContentObjectTypesLib.attachmentType),
                resolve: function (env) {
                    return Object.keys(env.source.attachments).map(function (key) {
                        return env.source.attachments[key];
                    });
                }
            },
            publish: {
                type: graphqlContentObjectTypesLib.publishInfoType,
                resolve: function (env) {
                    return env.source.publish;
                }
            }
        }
    };
    return graphQlLib.createObjectType(createContentTypeTypeParams);
}

function generateContentDataObjectType(contentType) {
    log.info('contentType:' + JSON.stringify(contentType, null, 2));
    var createContentTypeDataTypeParams = {
        name: generateCamelCase(contentType.displayName, true) + '_Data',
        description: contentType.displayName + ' data',
        fields: {}
    };
    
    //For each item of the content type form
    getFormItems(contentType.form).forEach(function (formItem) {
        
        //Creates a data field corresponding to this form item
        createContentTypeDataTypeParams.fields[sanitizeText(formItem.name)] = {
            type: generateFormItemObjectType(formItem),
            resolve: generateFormItemResolveFunction(formItem)
        }
    });
    return graphQlLib.createObjectType(createContentTypeDataTypeParams);
}

function getFormItems(form) {
    var formItems = [];
    form.forEach(function(formItem) {
        if ('ItemSet' === formItem.formItemType && formItem.items.length === 0) {
            return;
        }
        if ('Layout' === formItem.formItemType) {
            getFormItems(formItem.items).forEach(function(layoutItem) {
               formItems.push(layoutItem); 
            });
            return;
        }
        formItems.push(formItem);
    }); 
    return formItems;
}

function generateFormItemObjectType(formItem) {
    var formItemObjectType;
    switch (formItem.formItemType) {
    case 'ItemSet':
        formItemObjectType = generateItemSetObjectType(formItem);
        break;
    case 'Layout':
        //Should already be filtered
        break;
    case 'Input':
        formItemObjectType = generateInputObjectType(formItem);
        break;
    case 'OptionSet':
        formItemObjectType = generateOptionSetObjectType(formItem);
        break;
    }

    formItemObjectType = formItemObjectType || graphQlLib.GraphQLString;
    if (formItem.occurrences && formItem.occurrences.maximum == 1) {
        return formItemObjectType;
    } else {
        return graphQlLib.list(formItemObjectType)
    }
}

function generateItemSetObjectType(itemSet) {
    var createItemSetTypeParams = {
        name: generateCamelCase(itemSet.label, true) + '_' + generateRandomString(),
        description: itemSet.label,
        fields: {}
    };
    getFormItems(itemSet.items).forEach(function (item) {
        createItemSetTypeParams.fields[generateCamelCase(item.name)] = {
            type: generateFormItemObjectType(item),
            resolve: generateFormItemResolveFunction(item)
        }
    });
    return graphQlLib.createObjectType(createItemSetTypeParams);
}

function generateInputObjectType(input) {
    switch (input.inputType) {
    case 'CheckBox':
        return graphQlLib.GraphQLBoolean;
    case 'ComboBox':
        return graphQlLib.GraphQLString;
    case 'ContentSelector':
        return graphQlLib.GraphQLID; //TODO ID or String?
    case 'CustomSelector':
        return graphQlLib.GraphQLString;
    case 'ContentTypeFilter':
        return graphQlLib.GraphQLString;
    case 'Date':
        return graphQlLib.GraphQLString; //TODO Date custom scalar type
    case 'DateTime':
        return graphQlLib.GraphQLString; //TODO DateTime custom scalar type
    case 'Double':
        return graphQlLib.GraphQLFloat;
    case 'MediaUploader':
        return graphQlLib.GraphQLID; //TODO ID or String?
    case 'AttachmentUploader':
        return graphQlLib.GraphQLID; //TODO ID or String?
    case 'GeoPoint':
        return graphqlContentObjectTypesLib.geoPointType;
    case 'HtmlArea':
        return graphQlLib.GraphQLString;
    case 'ImageSelector':
        return graphQlLib.GraphQLID;
    case 'ImageUploader':
        return graphqlContentObjectTypesLib.mediaUploaderType;
    case 'Long':
        return graphQlLib.GraphQLInt;
    case 'RadioButton':
        return graphQlLib.GraphQLString; //TODO Should be enum based on config
    case 'SiteConfigurator':
        return graphqlContentObjectTypesLib.siteConfiguratorType;
    case 'Tag':
        return graphQlLib.GraphQLString;
    case 'TextArea':
        return graphQlLib.GraphQLString;
    case 'TextLine':
        return graphQlLib.GraphQLString;
    case 'Time':
        return graphQlLib.GraphQLString; //TODO Time custom scalar type
    }
    return graphQlLib.GraphQLString;
}

function generateOptionSetObjectType(optionSet) {
    var typeName = generateCamelCase(optionSet.label, true) + '_' + generateRandomString();
    var optionSetEnum = generateOptionSetEnum(optionSet, typeName);
    var createOptionSetTypeParams = {
        name: typeName,
        description: optionSet.label,
        fields: {
            _selected: {
                type: optionSet.selection.maximum == 1 ? optionSetEnum : graphQlLib.list(optionSetEnum),
                resolve: optionSet.selection.maximum == 1 ? function (env) { //TODO Fix
                    return env.source._selected;
                } : function (env) {
                    return utilLib.forceArray(env.source._selected);
                }
            }
        }
    };
    optionSet.options.forEach(function (option) {
        createOptionSetTypeParams.fields[generateCamelCase(option.name)] = {
            type: generateOptionObjectType(option),
            resolve: function (env) {
                return env.source[option.name];
            }
        }
    });
    return graphQlLib.createObjectType(createOptionSetTypeParams);
}

function generateOptionSetEnum(optionSet, optionSetName) {
    var enumValues = {};
    optionSet.options.forEach(function (option) {
        enumValues[option.name] = option.name;
    });
    return graphQlLib.createEnumType({
        name: optionSetName + '_OptionEnum',
        description: optionSet.label + ' option enum.',
        values: enumValues
    });
}

function generateOptionObjectType(option) {
    if (option.items.length > 0) {
        return generateItemSetObjectType(option);
    } else {
        return graphQlLib.GraphQLString;
    }

}

function generateFormItemResolveFunction(formItem) {    
    if (formItem.occurrences && formItem.occurrences.maximum == 1) {
        return function (env) {
            return env.source[formItem.name];
        };
    } else {
        return function (env) {
            return utilLib.forceArray(env.source[formItem.name]);
        };
    }

}

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

function generateRandomString() {
    return Math.random().toString(36).substr(2, 10).toUpperCase() + Math.random().toString(36).substr(2, 6).toUpperCase();
}


