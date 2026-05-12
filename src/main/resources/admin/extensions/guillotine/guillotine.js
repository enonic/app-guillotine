const adminLib = require('/lib/xp/admin');
const mustache = require('/lib/mustache');
const corsLib = require('/lib/cors');
const staticLib = require('/lib/enonic/static');
const routerLib = require('/lib/router')();

const STATIC_BASE_PATH = '/_static';

exports.all = function (req) {
    return routerLib.dispatch(req);
};

routerLib.get(`${STATIC_BASE_PATH}/{path:.*}`, (request) => {
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

routerLib.get('/?', function (req) {
    const view = resolve('guillotine.html');

    const extensionUrl = adminLib.extensionUrl({
        application: 'com.enonic.app.guillotine',
        extension: 'guillotine'
    });

    const params = {
        playgroundCss: `${extensionUrl}${STATIC_BASE_PATH}/styles/main.css`,
        playgroundScript: `${extensionUrl}${STATIC_BASE_PATH}/js/main.js`,
        handlerUrl: extensionUrl,
    };

    return {
        contentType: 'text/html',
        headers: corsLib.getHeaders(req),
        body: mustache.render(view, params),
    };
});
