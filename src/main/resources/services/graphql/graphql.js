var graphQlLib = require('/lib/graphql');

var schemaLib = require('/lib/guillotine/schema');
var securityLib = require('/lib/guillotine/security');

var graphqlPlaygroundLib = require('/lib/graphql-playground');

exports.post = function (req) {
    if (!securityLib.isSiteContext()) {
        return createNotFoundError();
    } else if (!securityLib.isAllowedSiteContext()) {
        return createForbiddenError();
    }

    var body = JSON.parse(req.body);
    var result = graphQlLib.execute(schemaLib.getSchema(req), body.query, body.variables);
    return {
        contentType: 'application/json',
        body: result
    };
};

exports.get = function (req) {
    var body = graphqlPlaygroundLib.render();
    return {
        contentType: 'text/html; charset=utf-8',
        body: body
    };
};

function createNotFoundError() {
    return {
        status: 404,
        body: {
            "errors": [
                {
                    "errorType": "404",
                    "message": "Not found"
                }
            ]
        }
    }
}

function createForbiddenError() {
    return {
        status: 403,
        body: {
            "errors": [
                {
                    "errorType": "403",
                    "message": "Forbidden"
                }
            ]
        }
    }
}