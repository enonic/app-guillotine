var eventLib = require('/lib/xp/event');
var graphQlLib = require('/lib/graphql');
var namingLib = require('/lib/headless-cms/naming');
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

var schema = null;
exports.getSchema = function () {
    if (!schema) {
        schema = createSchema();
    }
    return schema;
};

function createSchema() {
    return graphQlLib.createSchema({
        query: graphQlRootQueryLib.createRootQueryType()
    })
};

function invalidateSchema() {
    schema = null;
    namingLib.resetNameSet();
}

