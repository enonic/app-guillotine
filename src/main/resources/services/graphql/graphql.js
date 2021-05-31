const guillotineLib = require('/lib/guillotine');
const securityLib = require('/lib/guillotine/util/security');
const graphqlPlaygroundLib = require('/lib/graphql-playground');
const contentLib = require('/lib/xp/content');
const contextLib = require('/lib/xp/context');
const portalLib = require('/lib/xp/portal');

function createNotFoundError() {
    return {
        status: 404,
        body: {
            "errors": [
                {
                    "errorType": "404",
                    "message": "Not found"
                }
            ]
        }
    }
}

function createForbiddenError() {
    return {
        status: 403,
        body: {
            "errors": [
                {
                    "errorType": "403",
                    "message": "Forbidden"
                }
            ]
        }
    }
}

exports.post = function (req) {
    if (!securityLib.isSiteContext()) {
        return createNotFoundError();
    } else if (!securityLib.isAllowedSiteContext()) {
        return createForbiddenError();
    }

    let siteConfig = contextLib.run({
        branch: req.branch
    }, () => contentLib.getSiteConfig({
        key: portalLib.getSite()._id,
        applicationKey: 'com.enonic.app.guillotine'
    }));

    let input = JSON.parse(req.body);

    let params = {
        query: input.query,
        variables: input.variables,
        schemaOptions: {
            applications: siteConfig.applications,
            allowPaths: siteConfig.allowPaths,
            subscriptionEventTypes: siteConfig.subscriptionEventTypes
        }
    };

    return {
        contentType: 'application/json',
        body: guillotineLib.execute(params)
    };
};

exports.get = function (req) {
    if (req.webSocket) {
        return {
            webSocket: {
                data: guillotineLib.createWebSocketData(req),
                subProtocols: ['graphql-ws']
            }
        };
    }

    let body = graphqlPlaygroundLib.render();
    return {
        contentType: 'text/html; charset=utf-8',
        body: body
    };
};

exports.webSocketEvent = guillotineLib.initWebSockets();
