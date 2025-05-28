/* global log, Java */

const corsLib = require('/lib/cors');
const mustacheLib = require('/lib/mustache');
const appLib = require('/lib/xp/app');
const portalLib = require('/lib/xp/portal');
const contextLib = require('/lib/xp/context');

const helper = __.newBean('com.enonic.app.guillotine.helper.AppHelper');

const schemaLib = require('/lib/schema');

const getStaticUrl = (path) => `${portalLib.serviceUrl({service: 'static'})}/${path}`;
// const getStaticUrl = (path) => `${portalLib.apiUrl({api: 'static'})}/${path}`;

exports.options = function (req) {
    return {
        status: 204,
        headers: corsLib.getHeaders(req),
    }
};

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
        const view = resolve('graphql.html');

        const normalizedUrl = normalizeUrl(req.url);
        const params = {
            wsUrl: normalizedUrl.replace('http', 'ws'),
            handlerUrl: normalizedUrl,
            playgroundCss: getStaticUrl('styles/query-playground.css'),
            playgroundScript: getStaticUrl('js/query-playground.js'),
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
        headers: corsLib.getHeaders(req),
        body: contextLib.run({
            branch: req.params.branch,
        }, function () {
            return schemaLib.executeGraphQLQuery(input.query, input.variables);
        }),
    };
}
