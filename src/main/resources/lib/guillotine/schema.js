var eventLib = require('/lib/xp/event');
var portalLib = require('/lib/xp/portal');
var graphQlLib = require('/lib/graphql');

var guillotineLib = require('./guillotine');

var graphQlRootQueryLib = require('./root-query');

eventLib.listener({
    type: 'application',
    localOnly: false,
    callback: function (event) {
        if ('STOPPED' === event.data.eventType || 'STARTED' === event.data.eventType) {
            invalidate();
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
                nodes.forEach(function (node) {
                    invalidate(node.id + '/' + node.branch);
                });
            }
        }

    }
});

var schemaMap = {};
exports.getSchema = function (req) {
    var schemaId = getSchemaId(req);
    var schema;
    Java.type('com.enonic.app.guillotine.Synchronizer').sync(__.toScriptValue(function() {
        schema = schemaMap[schemaId];
        if (!schema) {
            schema = createSchema();
            schemaMap[schemaId] = schema;
        }     
    }));    
    return schema;
};

function getSchemaId(req) {
    var siteId = portalLib.getSite()._id;
    var branch = req.branch;
    return siteId + '/' + branch;
}

function createSchema() {
    var context = guillotineLib.createContext();
    return graphQlLib.createSchema({
        query: graphQlRootQueryLib.createRootQueryType(context),
        dictionary: context.dictionary
    });
}

function invalidate(schemaId) {
    if (schemaId) {
        delete schemaMap[schemaId];
    } else {
        schemaMap = {};    
    }    
}

