var graphQlLib = require('/lib/graphql');
var schemaLib = require('./schema');

exports.post = function (req) {
    var body = JSON.parse(req.body);
    
    var result = graphQlLib.execute(schemaLib.getSchema(), body.query, body.variables);
    return {
        contentType: 'application/json',
        body: result
    };
};