const mustache = require('/lib/mustache');

exports.get = function (req) {
    const view = resolve('guillotine.html');
    const assetsUrl = portalLib.assetUrl({path: ""});
    const wsUrl = portalLib.url({
        path: '/',
        type: 'websocket'
    });

    const params = {
        assetsUrl: assetsUrl,
        wsUrl: wsUrl.endsWith('/') ? wsUrl.substring(0, wsUrl.length - 1) : wsUrl
    };

    return {
        contentType: 'text/html',
        body: mustache.render(view, params),
    };
}

exports.get = handleGet;
