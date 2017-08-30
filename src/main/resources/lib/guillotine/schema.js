var eventLib = require('/lib/xp/event');
var portalLib = require('/lib/xp/portal');
var graphQlLib = require('/lib/graphql');

var contentTypesLib = require('./content-types');
var enumTypesLib = require('./enum-types');
var inputTypesLib = require('./input-types');
var genericTypesLib = require('./generic-types');
var namingLib = require('./naming');
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

var contextMap = createContext();
exports.getSchema = function () {
    var siteId = portalLib.getSite()._id;
    var context = contextMap[siteId];
    if (!context) {
        context = createContext();
        contextMap[siteId] = context;
        createSchema(context);
    }
    return context.schema;
};

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
                uniqueName = name + '_' + namingLib.generateRandomString();
            }
            this.nameSet[uniqueName] = true;
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

