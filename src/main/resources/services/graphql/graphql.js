var eventLib = require('/lib/xp/event');
var graphQlLib = require('/lib/graphql');
var schemaLib = require('./schema');
var namingLib = require('/lib/headless-cms/naming');


var schema = null;
eventLib.listener({
    type: 'application',
    localOnly: false,
    callback: function (event) {
        if ('STOPPED' === event.data.eventType || 'STARTED' === event.data.eventType) {
            schema = null;
            namingLib.resetNameSet();
        }
    }
});

exports.post = function (req) {
    var body = JSON.parse(req.body);
    
    if (!schema) {
        schema = schemaLib.createSchema();
    }
    
    var result = graphQlLib.execute(schema, body.query, body.variables);
    return {
        contentType: 'application/json',
        body: result
    };
};