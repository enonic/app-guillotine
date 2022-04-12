const portalLib = require('/lib/xp/portal');
const mustacheLib = require('/lib/mustache');

exports.get = function (req) {
    const view = resolve('guillotine.html');
    const assetsUrl = portalLib.assetUrl({path: ""});

    const params = {
        assetsUrl: assetsUrl
    };
    return {
        body: mustacheLib.render(view, params),
        contentType: 'text/html'
    };
};
