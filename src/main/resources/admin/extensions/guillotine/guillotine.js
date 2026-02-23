const mustache = require('/lib/mustache');
const portalLib = require('/lib/xp/portal');
const contextLib = require('/lib/xp/context');
const schemaLib = require('../../../lib/schema');
const corsLib = require('../../../lib/cors');

const staticLib = require('/lib/enonic/static');
const router = require('/lib/router')();

const BASE_PATH = '/com.enonic.app.guillotine/guillotine';
const STATIC_BASE_PATH = `${BASE_PATH}/_static`;

exports.all = function (req) {
    return router.dispatch(req);
};

router.get(`${STATIC_BASE_PATH}/{path:.*}`, (request) => {
    return staticLib.requestHandler(
        request,
        {
            cacheControl: () => staticLib.RESPONSE_CACHE_CONTROL.SAFE,
            index: false,
            root: '/assets',
            relativePath: staticLib.mappedRelativePath(STATIC_BASE_PATH),
        }
    );
});

router.get(`${BASE_PATH}/?`, (request) => {
    const view = resolve('guillotine.html');

    const handlerUrl = portalLib.apiUrl({
        api: 'admin:extension',
        path: ['com.enonic.app.guillotine', 'guillotine'],
    });

    const params = {
        playgroundCss: `${handlerUrl}/_static/styles/main.css`,
        playgroundScript: `${handlerUrl}/_static/js/main.js`,
        handlerUrl: handlerUrl,
    };

    return {
        contentType: 'text/html',
        body: mustache.render(view, params),
    };
});

router.post(`${BASE_PATH}/?`, (req) => {
    const input = JSON.parse(req.body);

    return {
        contentType: 'application/json',
        headers: corsLib.getHeaders(req),
        body: contextLib.run({
            repository: `com.enonic.cms.${req.params.project}`,
            branch: req.params.branch,
        }, function () {
            return schemaLib.executeGraphQLQuery(input.query, input.variables);
        }),
    };
});
