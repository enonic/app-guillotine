const mustache = require('/lib/mustache');
const portalLib = require('/lib/xp/portal');
const corsLib = require('/lib/cors');

exports.get = function (req) {
    const view = resolve('guillotine.html');
    const assetsUrl = portalLib.assetUrl({path: ""}); // TODO switch to lib-static
    const baseUrl = '/admin/site/preview/';
    const wsUrl = portalLib.url({
        path: baseUrl,
        type: 'websocket',
    });
    const handlerUrl = portalLib.url({
        path: baseUrl,
    });

    const params = {
        assetsUrl: assetsUrl,
        wsUrl: wsUrl,
        handlerUrl: handlerUrl,
    };

    return {
        contentType: 'text/html',
        headers: corsLib.getHeaders(req),
        body: mustache.render(view, params),
    };
}

