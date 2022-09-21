/* global log, Java */

const guillotineLib = require('/lib/guillotine');
const webSocketLib = require('/lib/xp/websocket');
const graphQlRxLib = require('/lib/graphql-rx');
const appLib = require('/lib/app');
const eventLib = require('/lib/xp/event');
const graphQlLib = require('/lib/graphql');

const CORS_HEADERS = {
    'Access-Control-Allow-Headers': 'Content-Type',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Access-Control-Allow-Origin': '*'
};

let schema;

eventLib.listener({
    type: 'application',
    localOnly: false,
    callback: function (event) {
        let eventType = event.data.eventType;
        if ('STOPPED' === eventType || 'STARTED' === eventType || 'UNINSTALLED' === eventType) {
            Java.type('com.enonic.app.guillotine.Synchronizer').sync(__.toScriptValue(function () {
                schema = null;
            }));

            eventLib.send({
                type: 'com.enonic.app.guillotine-schemaChanged',
                distributed: true
            });
        }
    }
});

function getSchema() {
    if (!schema) {
        Java.type('com.enonic.app.guillotine.Synchronizer').sync(__.toScriptValue(function () {
            schema = guillotineLib.createSchema({
                applications: appLib.getInstalledApplications().applications
            });
        }));
    }
    return schema;
}

function createQueryContext(headers) {
    let siteKey = null;
    Object.keys(headers).every(header => {
        if ('x-guillotine-sitekey' === header.toLowerCase()) {
            siteKey = headers[header];
            return false;
        }
        return true;
    });

    return {
        __siteKey: siteKey,
    }
}

exports.get = function (req) {
    if (!req.webSocket) {
        return {
            status: 404
        };
    }
    return {
        webSocket: {
            data: {
                branch: req.branch,
                repositoryId: req.repositoryId
            },
            subProtocols: ['graphql-transport-ws']
        }
    };
}

exports.post = function (req) {
    const input = JSON.parse(req.body);

    return {
        contentType: 'application/json',
        headers: CORS_HEADERS,
        body: JSON.stringify(graphQlLib.execute(getSchema(), input.query, input.variables, createQueryContext(req.headers)))
    };
}

const graphQlSubscribers = {};

exports.webSocketEvent = function (event) {
    log.debug(`WS event ${JSON.stringify(event)}`);

    if (!event) {
        return;
    }

    switch (event.type) {
    case 'message': {
        processMessageEvent(event);
        break;
    }
    case 'close': {
        cancelSubscription(event.session.id);
        break;
    }
    default: {
        log.debug(`Unknown event type ${event.type}`);
    }
    }
}

function cancelSubscription(sessionId) {
    Java.synchronized(() => {
        const subscriber = graphQlSubscribers[sessionId];
        if (subscriber) {
            delete graphQlSubscribers[sessionId];
            subscriber.cancelSubscription();
        }
    }, graphQlSubscribers)();
}

function processMessageEvent(event) {
    let message = JSON.parse(event.message);

    switch (message.type) {
    case 'connection_init':
        webSocketLib.send(event.session.id, JSON.stringify({
            type: 'connection_ack'
        }));
        break;
    case 'subscribe': {
        handleSubscribeMessage(event, message);
        break;
    }
    case 'complete': {
        cancelSubscription(event.session.id);
        break;
    }
    default: {
        log.debug(`Unknown message type ${message.type}`);
    }
    }
}

function handleSubscribeMessage(subscriptionEvent, message) {
    const operationId = message.id;
    const payload = message.payload;
    const sessionId = subscriptionEvent.session.id;
    try {
        // very important to use `graphQlLib.execute` instead of `guillotineLib.execute`,
        // otherwise the result will be wrapped by JSON.stringify(...)
        // and will not be possible to instanceof `data` to `Publisher` type
        const result = graphQlLib.execute(getSchema(), payload.query, payload.variables);

        if (result.data instanceof com.enonic.lib.graphql.rx.Publisher) {
            const subscriber = graphQlRxLib.createSubscriber({
                onNext: (payload) => {
                    if (payload.data.event.dataAsJson && payload.data.event.dataAsJson.nodes) {
                        Object.keys(payload.data.event.dataAsJson.nodes).forEach(key => {
                            let node = payload.data.event.dataAsJson.nodes[key];
                            if (node.repo === subscriptionEvent.data.repositoryId && node.branch === subscriptionEvent.data.branch) {
                                sendWSMsg(sessionId, operationId, payload);
                            }
                        });
                    } else {
                        sendWSMsg(sessionId, operationId, payload);
                    }
                }
            });
            Java.synchronized(() => graphQlSubscribers[sessionId] = subscriber, graphQlSubscribers)();
            result.data.subscribe(subscriber);
        }
    } catch (e) {
        log.error('Error while handling `subscribe` GRAPHQL-TRANSPORT-WS message', e);
        throw e;
    }
}

function sendWSMsg(sessionId, operationId, payload) {
    webSocketLib.send(sessionId, JSON.stringify({
        type: 'next',
        id: operationId,
        payload: payload
    }));
}
