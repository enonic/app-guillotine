const guillotineLib = require('/lib/guillotine');
const appLib = require('/lib/app');
const eventLib = require('/lib/xp/event');

let schema;

eventLib.listener({
    type: 'application',
    localOnly: false,
    callback: function (event) {
        let eventType = event.data.eventType;
        if ('STOPPED' === eventType || 'STARTED' === eventType || 'UNINSTALLED' === eventType) {
            Java.type('com.enonic.app.guillotine.Synchronizer').sync(__.toScriptValue(function () {
                schema = null;
            }));
        }
    }
});

function getSchema() {
    if (!schema) {
        Java.type('com.enonic.app.guillotine.Synchronizer').sync(__.toScriptValue(function () {
            schema = guillotineLib.createSchema({
                applications: appLib.getInstalledApplications().applications,
                creationCallbacks: {
                    'HeadlessCms': function (context, params) {
                        delete params.fields.getSite;
                    }
                },
                mode: 'project'
            });
        }));
    }
    return schema;
}

exports.post = function (req) {
    let input = JSON.parse(req.body);

    let params = {
        query: input.query,
        variables: input.variables,
        schema: getSchema()
    };
    return {
        contentType: 'application/json',
        body: guillotineLib.execute(params)
    };
}
