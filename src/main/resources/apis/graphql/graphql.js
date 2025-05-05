/* global log, Java */

const eventLib = require('/lib/xp/event');
const corsLib = require('/lib/cors');
const mustacheLib = require('/lib/mustache');
const staticLib = require('/lib/enonic/static');
const appLib = require('/lib/xp/app');

const graphQLApi = __.newBean('com.enonic.app.guillotine.graphql.GraphQLApi');
const syncExecutor = __.newBean('com.enonic.app.guillotine.Synchronizer');
const helper = __.newBean('com.enonic.app.guillotine.helper.AppHelper');

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

function getHeaders(req) {
    return corsLib.resolveHeaders(app.config, req);
}

exports.options = function (req) {
    return {
        status: 204,
        headers: getHeaders(req),
    }
};

const getStatic = staticLib.buildGetter(
    {
        root: 'assets',
        getCleanPath: request => {
            return request.rawPath.split('/_static/')[1];
        },
        cacheControl: 'no-cache',
        etag: true,
    }
);

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

exports.get = function (req) {
    if (!shouldBeRendered(req)) {
        return {
            status: 404,
        }
    } else {
        if (req.rawPath.indexOf('/_static/') !== -1) { // TODO lib router
            return getStatic(req);
        }

        const view = resolve('graphql.html');

        const normalizedUrl = normalizeUrl(req.url);
        const params = {
            wsUrl: normalizedUrl.replace('http', 'ws'),
            handlerUrl: normalizedUrl
        };

        return {
            status: 200,
            contentType: 'text/html',
            body: mustacheLib.render(view, params)
        };
    }
}

exports.post = function (req) {
    const input = JSON.parse(req.body);

    return {
        contentType: 'application/json',
        headers: getHeaders(req),
        body: JSON.stringify(__.toNativeObject(graphQLApi.execute(getSchema(), input.query, __.toScriptValue(input.variables))))
    };
}
