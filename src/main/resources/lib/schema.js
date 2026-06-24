/* global __, log, Java */

const eventLib = require('/lib/xp/event');

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

exports.executeGraphQLQuery = function (query, variables) {
    return JSON.stringify(__.toNativeObject(graphQLApi.execute(getSchema(), query, __.toScriptValue(variables))));
};
