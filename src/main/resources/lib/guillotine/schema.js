var eventLib = require('/lib/xp/event');
var contentLib = require('/lib/xp/content');
var contextLib = require('/lib/xp/context');
var portalLib = require('/lib/xp/portal');
var utilLib = require('/lib/guillotine/util/util');

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
exports.getSchema = function (params) {
    const schemaId = getSchemaId(params);
    let schema = null;
    Java.type('com.enonic.app.guillotine.Synchronizer').sync(__.toScriptValue(function () {
        schema = schemaMap[schemaId];
        if (!schema) {
            schema = createSchema(params);
            schemaMap[schemaId] = schema;
        }
    }));
    return schema;
};

function getSchemaId(params) {
    var siteId = params.req ? portalLib.getSite()._id : params.siteId;
    var branch = params.req ? params.req.branch : params.branch;
    return siteId + '/' + branch;
}

function createSchema(params) {
    const siteConfigs = utilLib.forceArray(getSite(params).data.siteConfig);
    const applicationKeys = siteConfigs.map((siteConfigEntry) => siteConfigEntry.applicationKey);

    const siteConfig = getSiteConfig(params);
    const allowPaths = utilLib.forceArray(siteConfig && siteConfig.allowPaths);

    return guillotineLib.createSchema({
        applications: applicationKeys,
        allowPaths: allowPaths
    });
}

function getSite(params) {
    if (params.req) {
        return portalLib.getSite();
    }

    return contextLib.run({
        branch: params.branch
    }, () => contentLib.get({key: params.siteId}));
}

function getSiteConfig(params) {
    if (params.req) {
        return portalLib.getSiteConfig();
    }

    return contextLib.run({
        branch: params.branch
    }, () => contentLib.getSiteConfig({key: params.siteId}));
}

function invalidate(schemaId) {
    if (schemaId) {
        delete schemaMap[schemaId];
    } else {
        schemaMap = {};
    }
}

