var graphQlLib = require('/lib/graphql');
var graphQlConnectionLib = require('/lib/graphql-connection');
var contentLib = require('/lib/xp/content');
var namingLib = require('/lib/headless-cms/naming');

exports.generateGenericContentFields = function () {
    return {
        _id: {
            type: graphQlLib.nonNull(graphQlLib.GraphQLID)
        },
        _name: {
            type: graphQlLib.nonNull(graphQlLib.GraphQLString)
        },
        _path: {
            type: graphQlLib.nonNull(graphQlLib.GraphQLString)
        },
        creator: {
            type: exports.principalKeyType
        },
        modifier: {
            type: exports.principalKeyType
        },
        createdTime: {
            type: graphQlLib.GraphQLString
        },
        modifiedTime: {
            type: graphQlLib.GraphQLString
        },
        owner: {
            type: exports.principalKeyType
        },
        type: {
            type: exports.schemaNameType
        },
        displayName: {
            type: graphQlLib.GraphQLString
        },
        hasChildren: {
            type: graphQlLib.GraphQLBoolean
        },
        language: {
            type: graphQlLib.GraphQLString
        },
        valid: {
            type: graphQlLib.GraphQLBoolean
        },
        x: {
            type: graphQlLib.list(exports.extraDataType),
            resolve: function (env) {
                var extraDatas = [];
                Object.keys(env.source.x).forEach(function (applicationKey) {
                    var applicationExtraData = env.source.x[applicationKey];
                    Object.keys(applicationExtraData).forEach(function (mixinLocalName) {
                        var mixin = applicationExtraData[mixinLocalName];
                        extraDatas.push({name: applicationKey + ':' + mixinLocalName, data: mixin});
                    });
                });
                return extraDatas;
            }
        },
        page: {
            type: exports.pageType
        },
        attachments: {
            type: graphQlLib.list(exports.attachmentType),
            resolve: function (env) {
                return Object.keys(env.source.attachments).map(function (key) {
                    return env.source.attachments[key];
                });
            }
        },
        publish: {
            type: exports.publishInfoType
        },
        site: {
            type: graphQlLib.reference('Site'),
            resolve: function (env) {
                return contentLib.getSite({key: env.source._id});
            }
        },
        parent: {
            type: graphQlLib.reference('Content'),
            resolve: function (env) {
                if (env.source._path === '/' || env.source._path === '/content') { //TODO Incorrect path
                    return null;
                } else {
                    var lastSlashIndex = env.source._path.lastIndexOf('/');
                    var parentPath = lastSlashIndex == 0 ? '/' : env.source._path.substr(0, lastSlashIndex);
                    return contentLib.get({key: parentPath});
                }
            }
        },
        children: {
            type: graphQlLib.list(graphQlLib.reference('Content')),
            args: {
                offset: graphQlLib.GraphQLInt,
                first: graphQlLib.GraphQLInt,
                sort: graphQlLib.GraphQLString
            },
            resolve: function (env) {
                return contentLib.getChildren({
                    key: env.source._id,
                    start: env.args.offset,
                    count: env.args.first,
                    sort: env.args.sort
                }).hits;
            }
        },
        permissions: {
            type: graphQlLib.reference('Permissions'),
            resolve: function (env) {
                return contentLib.getPermissions({
                    key: env.source._id
                });
            }
        }
    };
};

exports.principalKeyType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('PrincipalKey'),
    description: 'Principal key.',
    fields: {
        value: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source;
            }
        },
        type: {
            type: graphQlLib.createEnumType({
                name: 'PrincipalType',
                description: 'Principal type.',
                values: {
                    'user': 'user',
                    'group': 'group',
                    'role': 'role'
                }
            }),
            resolve: function (env) {
                return env.source.split(':', 2)[0];
            }
        },
        userStore: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.split(':', 3)[1];
            }
        },
        principalId: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.split(':', 3)[2];
            }
        }
    }
});

exports.permissionType = graphQlLib.createEnumType({
    name: 'Permission',
    description: 'Permission.',
    values: {
        'READ': 'READ',
        'CREATE': 'CREATE',
        'MODIFY': 'MODIFY',
        'DELETE': 'DELETE',
        'PUBLISH': 'PUBLISH',
        'READ_PERMISSIONS': 'READ_PERMISSIONS',
        'WRITE_PERMISSIONS': 'WRITE_PERMISSIONS'
    }
});

exports.accessControlEntryType = graphQlLib.createObjectType({
    name: 'AccessControlEntry',
    description: 'Access control entry.',
    fields: {
        principal: {
            type: graphQlLib.reference('PrincipalKey')
        },
        allow: {
            type: graphQlLib.list(exports.permissionType)
        },
        deny: {
            type: graphQlLib.list(exports.permissionType)
        }
    }
});

exports.schemaNameType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('SchemaName'),
    description: 'Schema name type.',
    fields: {
        value: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source;
            }
        },
        applicationKey: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.split(':', 2)[0];
            }
        },
        localName: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.split(':', 2)[1];
            }
        }
    }
});

exports.geoPointType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('GeoPoint'),
    description: 'GeoPoint.',
    fields: {
        value: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source;
            }
        },
        latitude: {
            type: graphQlLib.GraphQLFloat,
            resolve: function (env) {
                return env.source.split(',', 2)[0];
            }
        },
        longitude: {
            type: graphQlLib.GraphQLFloat,
            resolve: function (env) {
                return env.source.split(',', 2)[1];
            }
        }
    }
});

exports.mediaFocalPointType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('MediaFocalPoint'),
    description: 'Media focal point.',
    fields: {
        x: {
            type: graphQlLib.GraphQLFloat
        },
        y: {
            type: graphQlLib.GraphQLFloat
        }
    }
});

exports.mediaUploaderType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('MediaUploader'),
    description: 'Media uploader.',
    fields: {
        attachment: {
            type: graphQlLib.GraphQLString
        },
        focalPoint: {
            type: exports.mediaFocalPointType
        }
    }
});

exports.siteConfiguratorType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('SiteConfigurator'),
    description: 'Site configurator.',
    fields: {
        applicationKey: {
            type: graphQlLib.GraphQLString
        },
        config: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return JSON.stringify(env.source.config);
            }
        }
    }
});

exports.publishInfoType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('PublishInfo'),
    description: 'Publish information.',
    fields: {
        from: {
            type: graphQlLib.GraphQLString
        },
        to: {
            type: graphQlLib.GraphQLString
        },
        first: {
            type: graphQlLib.GraphQLString
        }
    }
});

exports.attachmentType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('Attachment'),
    description: 'Attachment.',
    fields: {
        name: {
            type: graphQlLib.GraphQLString
        },
        label: {
            type: graphQlLib.GraphQLString
        },
        size: {
            type: graphQlLib.GraphQLInt
        },
        mimeType: {
            type: graphQlLib.GraphQLString
        }
    }
});

exports.extraDataType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('ExtraData'),
    description: 'Extra data.',
    fields: {
        name: {
            type: exports.schemaNameType
        },
        data: {
            type: graphQlLib.GraphQLString
        }
    }
});

exports.componentType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('Component'),
    description: 'Component.',
    fields: {
        name: {
            type: graphQlLib.GraphQLString
        },
        path: {
            type: graphQlLib.GraphQLString
        },
        type: {
            type: graphQlLib.GraphQLString
        },
        descriptor: {
            type: graphQlLib.GraphQLString
        },
        text: {
            type: graphQlLib.GraphQLString
        },
        fragment: {
            type: graphQlLib.GraphQLString
        },
        config: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return JSON.stringify(env.source.config);
            }
        },
        regions: {
            type: graphQlLib.list(graphQlLib.reference('Region')),
            resolve: function (env) {
                return env.source.regions && Object.keys(env.source.regions).map(function (key) {
                        return env.source.regions[key];
                    });
            }
        }
    }
});

exports.pageRegionType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('Region'),
    description: 'Page region.',
    fields: {
        name: {
            type: graphQlLib.GraphQLString
        },
        components: {
            type: graphQlLib.list(exports.componentType)
        }
    }
});

exports.pageType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('Page'),
    description: 'Page.',
    fields: {
        template: {
            type: graphQlLib.GraphQLString
        },
        controller: {
            type: graphQlLib.GraphQLString
        },
        config: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return JSON.stringify(env.source.config);
            }
        },
        regions: {
            type: graphQlLib.list(exports.pageRegionType),
            resolve: function (env) {
                return env.source.regions && Object.keys(env.source.regions).map(function (key) {
                        return env.source.regions[key];
                    });
            }
        },
        fragment: {
            type: exports.componentType
        }
    }
});

var contentTypeObjectTypeMapping = {};
exports.registerContentTypeObjectType = function (type, contentTypeObjectType) {
    contentTypeObjectTypeMapping[type] = contentTypeObjectType;
};
exports.contentType = graphQlLib.createInterfaceType({
    name: namingLib.uniqueName('Content'),
    typeResolver: function (content) {
        return contentTypeObjectTypeMapping[content.type];
    },
    description: 'Content.',
    fields: exports.generateGenericContentFields()
});
exports.contentConnectionType = graphQlConnectionLib.createConnectionType(exports.contentType);


