const corsLib = require('/lib/enonic/cors');
const contextLib = require('/lib/xp/context');
const schemaLib = require('/lib/schema');
const router = require('/lib/router')();

exports.all = function (req) {
    return router.dispatch(req);
};

router.route('OPTIONS', '/?', (request) => {
    return {
        status: 204,
        headers: corsLib.respondOptions(request),
    };
});

router.post('/?', (req) => {
    const input = JSON.parse(req.body);

    return {
        contentType: 'application/json',
        headers: corsLib.getHeaders(req),
        body: contextLib.run({
            branch: req.params.branch,
        }, () => {
            return schemaLib.executeGraphQLQuery(input.query, input.variables);
        }),
    };
});
