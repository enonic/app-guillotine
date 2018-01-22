var eventLib = require('/lib/xp/event');
var portalLib = require('/lib/xp/portal');

var guillotineLib = require('/lib/guillotine');

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
    Java.type('com.enonic.app.guillotine.Synchronizer').sync(__.toScriptValue(function () {
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
    return guillotineLib.createSchema({
        applicationFilter: portalLib.getSiteConfig().applicationFilter || null
    });
}

function invalidate(schemaId) {
    if (schemaId) {
        delete schemaMap[schemaId];
    } else {
        schemaMap = {};
    }
}

