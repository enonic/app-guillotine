const corsLib = require('/lib/cors');
const schemaLib = require('/lib/schema');

function getHeaders(req) {
    return corsLib.getHeaders(req);
}

exports.options = function (req) {
    return {
        status: 204,
        headers: corsLib.respondOptions(req),
    }
};

exports.get = function (req) {
    return {
        status: 404,
        headers: getHeaders(req),
    };
}

exports.post = function (req) {
    const input = JSON.parse(req.body);

    return {
        contentType: 'application/json',
        headers: getHeaders(req),
        body: schemaLib.executeGraphQLQuery(input.query, input.variables),
    };
}
