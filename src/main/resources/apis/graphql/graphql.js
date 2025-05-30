/* global log, Java */

const corsLib = require('/lib/cors');
const mustacheLib = require('/lib/mustache');
const appLib = require('/lib/xp/app');
const contextLib = require('/lib/xp/context');
const helper = __.newBean('com.enonic.app.guillotine.helper.AppHelper');
const schemaLib = require('/lib/schema');
const staticLib = require('/lib/enonic/static');
const router = require('/lib/router')();

exports.all = function (req) {
    return router.dispatch(req);
};

router.route('OPTIONS', '/?', (request) => {
    return {
        status: 204,
        headers: corsLib.getHeaders(request),
    }
});

router.get(`/_static/{path:.*}`, (request) => {
    return staticLib.requestHandler(
        request,
        {
            cacheControl: () => staticLib.RESPONSE_CACHE_CONTROL.SAFE,
            index: false,
            root: '/assets',
            relativePath: staticLib.mappedRelativePath('/_static/'),
        }
    );
});

router.get('/?', (req) => {
    if (!shouldBeRendered(req)) {
        return {
            status: 404,
        }
    } else {
        const view = resolve('graphql.html');

        const normalizedUrl = normalizeUrl(req.url);
        const params = {
            wsUrl: normalizedUrl.replace('http', 'ws'),
            handlerUrl: normalizedUrl,
            playgroundCss: `${normalizedUrl}/_static/styles/query-playground.css`,
            playgroundScript: `${normalizedUrl}/_static/js/query-playground.js`,
        };

        return {
            status: 200,
            contentType: 'text/html',
            body: mustacheLib.render(view, params)
        };
    }
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

function shouldBeRendered(reg) {
    const isSDK = appLib.get({
        key: 'com.enonic.xp.app.welcome',
    }) !== null;
    const queryPlaygroundUIMode = (app.config['queryplayground.ui.mode'] || 'auto').toLowerCase();
    const uiCanBeRendered = isSDK || helper.isDevMode()
                            ? (queryPlaygroundUIMode === 'on' || queryPlaygroundUIMode === 'auto')
                            : queryPlaygroundUIMode === 'on';
    return !reg.webSocket && uiCanBeRendered;
}

function normalizeUrl(url) {
    return url.replace(/\/$/, '');
}
