const portalLib = require('/lib/xp/portal');
const mustacheLib = require('/lib/mustache');
const staticLib = require('/lib/enonic/static');
const routerLib = require('/lib/router')();

exports.all = function (req) {
    return routerLib.dispatch(req);
};

const getStatic = staticLib.buildGetter(
    {
        root: 'assets',
        getCleanPath: request => {
            return request.pathParams.resourcePath;
        },
        cacheControl: 'no-cache',
        etag: true,
    }
);

routerLib.get(
    '/site/[^/]+/_static/{resourcePath:.+}',
    request => {
        return getStatic(request);
    }
);

routerLib.get('/site/[^/]+', function (req) {
    const view = resolve('graphiql.html');

    const project = req.repositoryId ? req.repositoryId.replace('com.enonic.cms.', '') : 'default';
    const baseUrl = `/site/${project}`;

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
        staticResourceBaseUrl: `${staticResourceBaseUrl}/_static`,
    };

    return {
        status: 200,
        contentType: 'text/html',
        body: mustacheLib.render(view, params)
    };
});
