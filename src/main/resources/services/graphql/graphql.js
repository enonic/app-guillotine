var graphQlLib = require('/lib/graphql');
var graphQlRxLib = require('/lib/graphql-rx');
var authLib = require('/lib/xp/auth');
var portalLib = require('/lib/xp/portal');
var webSocketLib = require('/lib/xp/websocket');
var schemaLib = require('/lib/guillotine/schema');
var securityLib = require('/lib/guillotine/util/security');
var graphqlPlaygroundLib = require('/lib/graphql-playground');

exports.post = function (req) {
    if (!securityLib.isSiteContext()) {
        return createNotFoundError();
    } else if (!securityLib.isAllowedSiteContext()) {
        return createForbiddenError();
    }

    var body = JSON.parse(req.body);
    var result = graphQlLib.execute(schemaLib.getSchema({req: req}), body.query, body.variables);
    return {
        contentType: 'application/json',
        body: JSON.stringify(result)
    };
};


exports.get = function (req) {
    if (req.webSocket) {
        if (!isAuthenticated()) {
            return createUnauthorizedError();
        }
        if (!canAccessAdminLogin()) {
            return createForbiddenError();
        }
        return {
            webSocket: {
                data: {
                    branch: req.branch,
                    siteId: portalLib.getSite()._id

                },
                subProtocols: ['graphql-ws']
            }
        };
    }

    var body = graphqlPlaygroundLib.render();
    return {
        contentType: 'text/html; charset=utf-8',
        body: body
    };
};

const graphQlSubscribers = {};
exports.webSocketEvent = function (event) {
    log.debug('WebSocketEvent: ' + JSON.stringify(event));

    switch (event.type) {
    case 'close':
        cancelSubscription(event.session.id);
        break;
    case 'message':
        var message = JSON.parse(event.message);
        switch (message.type) { //https://github.com/apollographql/subscriptions-transport-ws/blob/master/PROTOCOL.md
        case 'connection_init':
            webSocketLib.send(event.session.id, JSON.stringify({
                type: 'connection_ack'
            }));
            break;
        case 'start':
            handleStartMessage(event, message);
            break;
        case 'stop':
            cancelSubscription(event.session.id);
            break;
        }
        break;
    case 'error':
        log.warning('Session [' + event.session.id + '] error: ' + event.error);
        break;
    }
};

function isAuthenticated() {
    return authLib.hasRole('system.authenticated')
}

function canAccessAdminLogin() {
    return authLib.hasRole('system.admin') || authLib.hasRole('system.admin.login')
}

function cancelSubscription(sessionId) {
    Java.synchronized(() => {
        const subscriber = graphQlSubscribers[sessionId];
        if (subscriber) {
            delete  graphQlSubscribers[sessionId];
            subscriber.cancelSubscription();
        }
    }, graphQlSubscribers)();
}

function handleStartMessage(event, message) {
    const sessionId = event.session.id;
    const graphlqlOperationId = message.id;
    const payload = message.payload;

    try {
        const schema = schemaLib.getSchema({
            branch: event.data.branch,
            siteId: event.data.siteId
        });
        const result = graphQlLib.execute(schema, payload.query, payload.variables);

        if (result.data instanceof com.enonic.lib.graphql.rx.Publisher) {
            const subscriber = graphQlRxLib.createSubscriber({
                onNext: (result) => {
                    webSocketLib.send(sessionId, JSON.stringify({
                        type: 'data',
                        id: graphlqlOperationId,
                        payload: result
                    }));
                }
            });
            Java.synchronized(() => graphQlSubscribers[sessionId] = subscriber, graphQlSubscribers)();
            result.data.subscribe(subscriber);
        }
    } catch (e) {
        log.error('Error while handling Start GraphQL-WS message', e);
        throw e;
    }
}

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

function createUnauthorizedError() {
    return {
        status: 401,
        body: {
            "errors": [
                {
                    "errorType": "401",
                    "message": "Unauthorized"
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
