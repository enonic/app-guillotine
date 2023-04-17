/* global log, Java */

const eventLib = require('/lib/xp/event');

const graphQLApi = __.newBean('com.enonic.app.guillotine.graphql.GraphQLApi');
const syncExecutor = __.newBean('com.enonic.app.guillotine.Synchronizer');

const CORS_HEADERS = {
    'Access-Control-Allow-Headers': 'Content-Type',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Access-Control-Allow-Origin': '*'
};

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

function createQueryContext(headers) {
    let siteKey = null;
    Object.keys(headers).every(header => {
        if ('x-guillotine-sitekey' === header.toLowerCase()) {
            siteKey = headers[header];
            return false;
        }
        return true;
    });

    return {
        __siteKey: siteKey,
    }
}

exports.get = function (req) {
    return {
        status: 404
    };
}

exports.post = function (req) {
    const input = JSON.parse(req.body);

    return {
        contentType: 'application/json',
        headers: CORS_HEADERS,
        body: JSON.stringify(__.toNativeObject(graphQLApi.execute(getSchema(), input.query, __.toScriptValue(input.variables),
            createQueryContext(req.headers))))
    };
}
