const mustache = require('/lib/mustache');
const portalLib = require('/lib/xp/portal');
const schemaLib = require('../../../lib/schema');
const corsLib = require('../../../lib/cors');

const getStaticUrl = (path) => `${portalLib.serviceUrl({service: 'static'})}/${path}`;

exports.get = function (req) {
    const view = resolve('guillotine.html');

    const wsUrl = portalLib.apiUrl({
        api: 'admin:widget',
        path: ['com.enonic.app.guillotine', 'guillotine'],
        type: 'websocket',
    });

    const handlerUrl = portalLib.apiUrl({
        api: 'admin:widget',
        path: ['com.enonic.app.guillotine', 'guillotine'],
    });

    const params = {
        playgroundCss: getStaticUrl('styles/main.css'),
        playgroundScript: getStaticUrl('js/main.js'),
        wsUrl: wsUrl,
        handlerUrl: handlerUrl,
    };

    return {
        contentType: 'text/html',
        body: mustache.render(view, params),
    };
}

exports.post = function (req) {
    const input = JSON.parse(req.body);

    return {
        contentType: 'application/json',
        headers: corsLib.getHeaders(req),
        body: schemaLib.executeGraphQLQuery(input.query, input.variables),
    };
}

