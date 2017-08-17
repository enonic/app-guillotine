var graphQlLib = require('/lib/graphql');
var graphQlConnectionLib = require('/lib/graphql-connection');
var contentLib = require('/lib/xp/content');
var portalLib = require('/lib/xp/portal');
var utilLib = require('./util');
var securityLib = require('./security');
var genericTypesLib = require('./generic-types');
var inputTypesLib = require('./input-types');
var namingLib = require('/lib/headless-cms/naming');

exports.addContentTypesAsFields = function (parentObjectTypeParams) {

    var allowedContentTypeRegexp = generateAllowedContentTypeRegexp();

    //For each content type
    contentLib.getTypes().
        filter(function (contentType) {
            return contentType.name.match(allowedContentTypeRegexp);
        }).
        forEach(function (contentType) {

            //Retrieve the content type  name as lower camel case
            var camelCaseContentTypeName = generateContentTypeGetter(contentType);

            //Generates the object type for this content type
            var contentTypeObjectType = generateContentTypeObjectType(contentType);

            //Creates a root query field getXXX finding a content by key 
            parentObjectTypeParams.fields[camelCaseContentTypeName] = {
                type: contentTypeObjectType,
                args: {
                    key: graphQlLib.nonNull(graphQlLib.GraphQLID)
                },
                resolve: function (env) {
                    var content = contentLib.getContent(env.args.key);
                    return content && content.type === contentType.name ? content : null;
                }
            };

            //Creates a root query field getXXXList finding contents and returning them as an array
            parentObjectTypeParams.fields[camelCaseContentTypeName + 'List'] = {
                type: graphQlLib.list(contentTypeObjectType),
                args: {
                    offset: graphQlLib.GraphQLInt,
                    first: graphQlLib.GraphQLInt,
                    query: graphQlLib.GraphQLString,
                    sort: graphQlLib.GraphQLString
                },
                resolve: function (env) {
                    var contents = contentLib.query({
                        start: env.args.offset,
                        count: env.args.first,
                        query: env.args.query,
                        sort: env.args.sort,
                        contentTypes: [contentType.name]
                    }).hits;
                    return contents;
                }
            };

            //Creates a root query field getXXXConnection finding contents
            parentObjectTypeParams.fields[camelCaseContentTypeName + 'Connection'] = {
                type: graphQlConnectionLib.createConnectionType(contentTypeObjectType),
                args: {
                    after: graphQlLib.GraphQLString,
                    first: graphQlLib.GraphQLInt,
                    query: graphQlLib.GraphQLString,
                    sort: graphQlLib.GraphQLString
                },
                resolve: function (env) {
                    var start = env.args.after ? parseInt(graphQlConnectionLib.decodeCursor(env.args.after)) + 1 : 0;
                    var queryResult = contentLib.query({
                        start: start,
                        count: env.args.first,
                        query: env.args.query,
                        sort: env.args.sort,
                        contentTypes: [contentType.name]

                    });
                    return {
                        total: queryResult.total,
                        start: start,
                        hits: queryResult.hits
                    };
                }
            };
        });
};

function generateAllowedContentTypeRegexp() {
    var siteApplicationKeys = portalLib.getSite().data.siteConfig.map(function (applicationConfigEntry) {
        return '|' + applicationConfigEntry.applicationKey.replace(/\./g, '\\.');
    }).join('');
    return new RegExp('^(?:base|media|portal' + siteApplicationKeys + '):');
}

function generateContentTypeGetter(contentType) {
    var localName = contentType.name.substr(contentType.name.indexOf(':') + 1);
    var camelCaseContentTypeName = namingLib.generateCamelCase(localName, true);
    return namingLib.uniqueName('get' + camelCaseContentTypeName);
}


function generateContentTypeObjectType(contentType) {
    var camelCaseDisplayName = namingLib.generateCamelCase(contentType.displayName, true);

    var createContentTypeTypeParams = {
        name: namingLib.uniqueName(camelCaseDisplayName),
        description: contentType.displayName + ' - ' + contentType.name,
        interfaces: [genericTypesLib.contentType],
        fields: genericTypesLib.generateGenericContentFields()
    };

    createContentTypeTypeParams.fields.data = getFormItems(contentType.form).length > 0 ? {
        type: generateContentDataObjectType(contentType)
    } : undefined;

    var contentTypeObjectType = graphQlLib.createObjectType(createContentTypeTypeParams);
    genericTypesLib.registerContentTypeObjectType(contentType.name, contentTypeObjectType);
    return contentTypeObjectType;
}

function generateContentDataObjectType(contentType) {
    var camelCaseDisplayName = namingLib.generateCamelCase(contentType.displayName + '_Data', true);
    var createContentTypeDataTypeParams = {
        name: namingLib.uniqueName(camelCaseDisplayName),
        description: contentType.displayName + ' data',
        fields: {}
    };

    //For each item of the content type form
    getFormItems(contentType.form).forEach(function (formItem) {

        //Creates a data field corresponding to this form item
        createContentTypeDataTypeParams.fields[namingLib.sanitizeText(formItem.name)] = {
            type: generateFormItemObjectType(formItem),
            args: generateFormItemArguments(formItem),
            resolve: generateFormItemResolveFunction(formItem)
        }
    });
    return graphQlLib.createObjectType(createContentTypeDataTypeParams);
}

function getFormItems(form) {
    var formItems = [];
    form.forEach(function (formItem) {
        if ('ItemSet' === formItem.formItemType && getFormItems(formItem.items).length === 0) {
            return;
        }
        if ('Layout' === formItem.formItemType) {
            getFormItems(formItem.items).forEach(function (layoutItem) {
                formItems.push(layoutItem);
            });
            return;
        }
        if ('Input' == formItem.formItemType && 'SiteConfigurator' == formItem.inputType) {
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
    var camelCaseLabel = namingLib.generateCamelCase(itemSet.label, true);
    var createItemSetTypeParams = {
        name: namingLib.uniqueName(camelCaseLabel),
        description: itemSet.label,
        fields: {}
    };
    getFormItems(itemSet.items).forEach(function (item) {
        createItemSetTypeParams.fields[namingLib.generateCamelCase(item.name)] = {
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
        return genericTypesLib.geoPointType;
    case 'HtmlArea':
        return graphQlLib.GraphQLString;
    case 'ImageSelector':
        return graphQlLib.GraphQLID;
    case 'ImageUploader':
        return genericTypesLib.mediaUploaderType;
    case 'Long':
        return graphQlLib.GraphQLInt;
    case 'RadioButton':
        return graphQlLib.GraphQLString; //TODO Should be enum based on config
    case 'SiteConfigurator':
        return genericTypesLib.siteConfiguratorType;
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
    var camelCaseLabel = namingLib.generateCamelCase(optionSet.label, true);
    var typeName = namingLib.uniqueName(camelCaseLabel);
    var optionSetEnum = generateOptionSetEnum(optionSet, typeName);
    var createOptionSetTypeParams = {
        name: typeName,
        description: optionSet.label,
        fields: {
            _selected: {
                type: optionSet.selection.maximum == 1 ? optionSetEnum : graphQlLib.list(optionSetEnum),
                resolve: optionSet.selection.maximum == 1 ? undefined : function (env) { //TODO Fix
                    return utilLib.forceArray(env.source._selected);
                }
            }
        }
    };
    optionSet.options.forEach(function (option) {
        createOptionSetTypeParams.fields[namingLib.generateCamelCase(option.name)] = {
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

function generateFormItemArguments(formItem) {
    var args = {};
    if (!formItem.occurrences || formItem.occurrences.maximum != 1) {
        args.offset = graphQlLib.GraphQLInt;
        args.first = graphQlLib.GraphQLInt;
    }
    if ('Input' == formItem.formItemType && 'HtmlArea' == formItem.inputType) {
        args.processHtml = inputTypesLib.createProcessHtmlInputType();
    }
    return args;
}

function generateFormItemResolveFunction(formItem) {
    if (formItem.occurrences && formItem.occurrences.maximum == 1) {
        return function (env) {
            var value = env.source[formItem.name];
            if (env.args.processHtml) {
                value = portalLib.processHtml({value: value, type: env.args.processHtml.type});
            }
            return value;
        };
    } else {
        return function (env) {
            var values = utilLib.forceArray(env.source[formItem.name]);
            if (env.args.offset != null || env.args.offset != null) {
                return values.slice(env.args.offset, env.args.first);
            }
            if (env.args.processHtml) {
                values = values.map(function (value) {
                    return portalLib.processHtml({value: value});
                });
            }
            return values;
        };
    }
}


