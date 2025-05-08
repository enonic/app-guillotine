const mustache = require('/lib/mustache');
const portalLib = require('/lib/xp/portal');
const assetLib = require('/lib/enonic/asset');

exports.get = function (req) {
    const view = resolve('guillotine.html');
    const assetsUrl = assetLib.assetUrl({path: ''});
    const baseUrl = '/admin/site/preview';
    const wsUrl = portalLib.url({
        path: baseUrl,
        type: 'websocket',
    });
    const handlerUrl = portalLib.apiUrl({
        api: 'graphql',
    });

    const params = {
        assetsUrl: assetsUrl,
        wsUrl: wsUrl,
        handlerUrl: handlerUrl,
    };

    return {
        contentType: 'text/html',
        body: mustache.render(view, params),
    };
}

