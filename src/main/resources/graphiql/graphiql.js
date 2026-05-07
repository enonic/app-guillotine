const portalLib = require('/lib/xp/portal');
const mustacheLib = require('/lib/mustache');
const staticLib = require('/lib/enonic/static');
const routerLib = require('/lib/router')();

const STATIC_BASE_PATH = '/_static';

exports.all = (req) => {
    return routerLib.dispatch(req);
};

routerLib.get(
    `${STATIC_BASE_PATH}/{resourcePath:.+}`,
    request => {
        return staticLib.requestHandler(
            request,
            {
                cacheControl: () => staticLib.RESPONSE_CACHE_CONTROL.SAFE,
                index: false,
                root: '/assets',
                relativePath: staticLib.mappedRelativePath(STATIC_BASE_PATH),
            }
        );
    }
);

routerLib.get('/?', (req) => {
    const view = resolve('graphiql.html');

    const baseUrl = req.contextPath;

    const wsUrl = portalLib.url({
        path: baseUrl,
        type: 'websocket',
    });
    const handlerUrl = portalLib.url({
        path: baseUrl,
    });

    const staticResourceBaseUrl = handlerUrl.replace(/\/$/, '');

    const params = {
        wsUrl: wsUrl,
        handlerUrl: handlerUrl,
        staticResourceBaseUrl: `${staticResourceBaseUrl}${STATIC_BASE_PATH}`,
    };

    return {
        status: 200,
        contentType: 'text/html',
        body: mustacheLib.render(view, params)
    };
});
