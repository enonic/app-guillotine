var eventLib = require('/lib/xp/event');
var portalLib = require('/lib/xp/portal');
var graphQlLib = require('/lib/graphql');
var namingLib = require('/lib/headless-cms/naming');
var genericTypesLib = require('./generic-types');
var graphQlRootQueryLib = require('./root-query');

eventLib.listener({
    type: 'application',
    localOnly: false,
    callback: function (event) {
        if ('STOPPED' === event.data.eventType || 'STARTED' === event.data.eventType) {
            invalidateSchema();
        }
    }
});

var schemaMap = {};
exports.getSchema = function () {
    var siteId = portalLib.getSite()._id;
    var schema = schemaMap[siteId];
    if (!schema) {
        schema = createSchema();
        schemaMap[siteId] = schema;
    }
    return schema;
};

function createSchema() {
    genericTypesLib.createGenericTypes();
    return graphQlLib.createSchema({
        query: graphQlRootQueryLib.createRootQueryType()
    })
};

function invalidateSchema() {
    schemaMap = {};
    namingLib.resetNameSet();
}

