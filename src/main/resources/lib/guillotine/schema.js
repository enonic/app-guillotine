var eventLib = require('/lib/xp/event');
var portalLib = require('/lib/xp/portal');
var graphQlLib = require('/lib/graphql');

var contentTypesLib = require('./content-types');
var dictionaryLib = require('./dictionary');
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

var contextMap = {};
exports.getSchema = function () {
    var siteId = portalLib.getSite()._id;
    var context = contextMap[siteId];
    if (!context) {
        context = {};
        contextMap[siteId] = context;
        createSchema(context);
    }
    return context.schema;
};

function createSchema(context) {
    genericTypesLib.createGenericTypes(context);
    contentTypesLib.createContentTypeTypes(context);
    context.schema = graphQlLib.createSchema({
        query: graphQlRootQueryLib.createRootQueryType(),
        dictionary: dictionaryLib.get()
    });
};

function invalidateContexts() {
    contextMap = {};
    namingLib.resetNameSet();
    dictionaryLib.reset();
}

