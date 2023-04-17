/* global __, log, Java */

const appLib = require('/lib/app');
const graphQLApi = __.newBean('com.enonic.app.guillotine.graphql.GraphQlApi');

const CORS_HEADERS = {
    'Access-Control-Allow-Headers': 'Content-Type',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Access-Control-Allow-Origin': '*'
};

let schema;

function getSchema() {
    if (!schema) {
        Java.type('com.enonic.app.guillotine.Synchronizer').sync(__.toScriptValue(function () {
            const paramsBuilder = __.newBean('com.enonic.app.guillotine.graphql.ParamsHelper').newInstance();
            paramsBuilder.setApplications(appLib.getInstalledApplications().applications);
            schema = graphQLApi.createSchema(paramsBuilder.build());
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

    const executor = __.newBean('com.enonic.app.guillotine.graphql.GraphQLExecutor');

    return {
        contentType: 'application/json',
        headers: CORS_HEADERS,
        body: JSON.stringify(__.toNativeObject(executor.execute(getSchema(), input.query, __.toScriptValue(input.variables),
            createQueryContext(req.headers))))
    };
}
