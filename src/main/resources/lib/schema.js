/* global __, log, Java */

const eventLib = require('/lib/xp/event');

const schemaProvider = __.newBean('com.enonic.app.guillotine.graphql.SchemaProviderBean');

eventLib.listener({
    type: 'application',
    localOnly: false,
    callback: function (event) {
        let eventType = event.data.eventType;
        if ('STOPPED' === eventType || 'STARTED' === eventType || 'UNINSTALLED' === eventType) {
            schemaProvider.invalidate();

            eventLib.send({
                type: 'com.enonic.app.guillotine-schemaChanged',
                distributed: true
            });
        }
    }
});

exports.executeGraphQLQuery = function (query, variables) {
    return JSON.stringify(__.toNativeObject(schemaProvider.execute(query, __.toScriptValue(variables))));
};
