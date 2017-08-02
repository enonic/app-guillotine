var graphQlLib = require('/lib/graphql');
var graphQlConnectionLib = require('/lib/graphql-connection');
var contentLib = require('/lib/xp/content');
var namingLib = require('/lib/headless-cms/naming');

exports.generateGenericContentFields = function () {
    return {
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
            type: exports.principalKeyType,
            resolve: function (env) {
                return env.source.creator;
            }
        },
        modifier: {
            type: exports.principalKeyType,
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
            type: exports.principalKeyType,
            resolve: function (env) {
                return env.source.owner;
            }
        },
        type: {
            type: exports.contentTypeNameType,
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
        x: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return JSON.stringify(env.source.x); //TODO
            }
        },
        page: {
            type: exports.pageType,
            resolve: function (env) {
                return env.source.page;
            }
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
            type: exports.publishInfoType,
            resolve: function (env) {
                return env.source.publish;
            }
        },
        site: {
            type: graphQlLib.reference('Site'),
            resolve: function (env) {
                return  contentLib.getSite({key: env.source._id});
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

exports.contentTypeNameType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('ContentTypeName'),
    description: 'Content type name.',
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
            type: graphQlLib.GraphQLFloat,
            resolve: function (env) {
                return env.source.x;
            }
        },
        y: {
            type: graphQlLib.GraphQLFloat,
            resolve: function (env) {
                return env.source.y;
            }
        }
    }
});

exports.mediaUploaderType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('MediaUploader'),
    description: 'Media uploader.',
    fields: {
        attachment: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.attachment;
            }
        },
        focalPoint: {
            type: exports.mediaFocalPointType,
            resolve: function (env) {
                return env.source.focalPoint;
            }
        }
    }
});

exports.siteConfiguratorType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('SiteConfigurator'),
    description: 'Site configurator.',
    fields: {
        applicationKey: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.applicationKey;
            }
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
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.from;
            }
        },
        to: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.to;
            }
        },
        first: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.first;
            }
        }
    }
});

exports.attachmentType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('Attachment'),
    description: 'Attachment.',
    fields: {
        name: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.name;
            }
        },
        label: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.label;
            }
        },
        size: {
            type: graphQlLib.GraphQLInt,
            resolve: function (env) {
                return env.source.size;
            }
        },
        mimeType: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.mimeType;
            }
        }
    }
});

exports.componentType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('Component'),
    description: 'Component.',
    fields: {
        name: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.name;
            }
        },
        path: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.path;
            }
        },
        type: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.type;
            }
        },
        descriptor: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.descriptor;
            }
        },
        text: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.text;
            }
        },
        fragment: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.fragment;
            }
        },
        config: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return JSON.stringify(env.source.config);
            }
        },
        regions: {
            type: graphQlLib.list(graphQlLib.reference('PageRegion')),
            resolve: function (env) {
                return env.source.regions && Object.keys(env.source.regions).map(function (key) {
                        return env.source.regions[key];
                    });
            }
        }
    }
});

exports.pageRegionType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('PageRegion'),
    description: 'Page region.',
    fields: {
        name: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.name;
            }
        },
        components: {
            type: graphQlLib.list(exports.componentType),
            resolve: function (env) {
                return env.source.components; //TODO
            }
        }
    }
});

exports.pageType = graphQlLib.createObjectType({
    name: namingLib.uniqueName('Page'),
    description: 'Page.',
    fields: {
        template: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.template;
            }
        },
        controller: {
            type: graphQlLib.GraphQLString,
            resolve: function (env) {
                return env.source.controller;
            }
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
            type: exports.componentType,
            resolve: function (env) {
                return env.source.fragment;
            }
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
