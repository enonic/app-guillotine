var eventLib = require('/lib/xp/event');
var portalLib = require('/lib/xp/portal');
var graphQlLib = require('/lib/graphql');

var contentTypesLib = require('./content-types');
var enumTypesLib = require('./enum-types');
var inputTypesLib = require('./input-types');
var genericTypesLib = require('./generic-types');
var graphQlRootQueryLib = require('./root-query');

eventLib.listener({
    type: 'application',
    localOnly: false,
    callback: function (event) {
        if ('STOPPED' === event.data.eventType || 'STARTED' === event.data.eventType) {
            invalidateContexts();
        }
    }
});

eventLib.listener({
    type: 'node.*',
    localOnly: false,
    callback: function (event) {
        if ('node.delete' === event.type || 'node.pushed' === event.type || 'node.updated' === event.type ||
            'node.stateUpdated' === event.type) {
            var nodes = event.data.nodes;
            if (nodes) {
                nodes.forEach(function(node) {
                    var contextId = node.id + '/' + node.branch;
                    delete contextMap[contextId];
                });
            }
        }

    }
});

var contextMap = {};
exports.getSchema = function (req) {
    var schemaId = getSchemaId(req);
    var context = contextMap[schemaId];
    if (!context) {
        context = createContext();
        contextMap[schemaId] = context;
        createSchema(context);
    }
    return context.schema;
};

function getSchemaId(req) {
    var siteId = portalLib.getSite()._id;
    var branch = req.branch;
    return siteId + '/' + branch;
}

function createContext() {
    return {
        types: {},
        dictionary: [],
        nameSet: {},
        contentTypeMap: {},
        addObjectType: function (objectType) {
            this.dictionary.push(objectType);
        },
        putContentType: function (name, objectType) {
            this.contentTypeMap[name] = objectType;
        },
        uniqueName: function (name) {
            var uniqueName = name;
            if (this.nameSet[name]) {
                this.nameSet[uniqueName]++;
                uniqueName = name + '_' + this.nameSet[uniqueName];
            } else {
                this.nameSet[uniqueName] = 1;
            }
            return uniqueName;
        }
    };
}

function createSchema(context) {
    enumTypesLib.createEnumTypes(context);
    inputTypesLib.createInputTypes(context);
    genericTypesLib.createGenericTypes(context);
    contentTypesLib.createContentTypeTypes(context);
    context.schema = graphQlLib.createSchema({
        query: graphQlRootQueryLib.createRootQueryType(context),
        dictionary: context.dictionary
    });
}

function invalidateContexts() {
    contextMap = {};
}

