/* global log, Java */

const eventLib = require('/lib/xp/event');
const corsLib = require('/lib/cors');

const graphQLApi = __.newBean('com.enonic.app.guillotine.graphql.GraphQLApi');
const syncExecutor = __.newBean('com.enonic.app.guillotine.Synchronizer');

let schema;

eventLib.listener({
    type: 'application',
    localOnly: false,
    callback: function (event) {
        let eventType = event.data.eventType;
        if ('STOPPED' === eventType || 'STARTED' === eventType || 'UNINSTALLED' === eventType) {
            syncExecutor.sync(__.toScriptValue(function () {
                schema = null;
            }));

            eventLib.send({
                type: 'com.enonic.app.guillotine-schemaChanged',
                distributed: true
            });
        }
    }
});

function getSchema() {
    if (!schema) {
        syncExecutor.sync(__.toScriptValue(function () {
            schema = graphQLApi.createSchema();
        }));
    }
    return schema;
}

function getHeaders(req) {
    return corsLib.resolveHeaders(app.config, req);
}

exports.options = function (req) {
    return {
        status: 204,
        headers: getHeaders(req),
    }
};

exports.get = function (req) {
    return {
        status: 404
    };
}

exports.post = function (req) {
    const input = JSON.parse(req.body);

    return {
        contentType: 'application/json',
        headers: getHeaders(req),
        body: JSON.stringify(__.toNativeObject(graphQLApi.execute(getSchema(), input.query, __.toScriptValue(input.variables))))
    };
}
