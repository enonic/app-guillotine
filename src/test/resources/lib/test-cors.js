const testingLib = require('/lib/xp/testing');
const corsLib = require('/lib/cors');

exports.testResolveWhenCorsHeadersDisabled = function (config, req) {
    const headers = corsLib.resolveHeaders(config, req);
    testingLib.assertJsonEquals({}, headers);
};

exports.testResolveDefaultCorsHeaders = function (config, req) {
    const headers = corsLib.resolveHeaders(config, req);
    testingLib.assertJsonEquals({
        'access-control-allow-origin': '*',
        'access-control-allow-headers': 'Content-Type',
        'access-control-allow-methods': 'POST, OPTIONS',
    }, headers);
};

exports.testResolveCorsHeaders = function (config, req) {
    const headers = corsLib.resolveHeaders(config, req);
    testingLib.assertJsonEquals({
        'access-control-allow-origin': 'http://test-cors.com:3000',
        'vary': 'Origin',
        'access-control-allow-credentials': 'true',
        'access-control-allow-headers': 'Content-Type, Authorization',
        'access-control-allow-methods': 'POST, OPTIONS, GET',
        'access-control-max-age': '1200',
    }, headers);
};

exports.testResolveCorsHeadersWithOriginFromRequest = function (config, req) {
    const headers = corsLib.resolveHeaders(config, req);
    testingLib.assertJsonEquals({
        'access-control-allow-origin': 'http://test-cors.com:3000',
        'vary': 'Origin',
        'access-control-allow-credentials': 'true',
        'access-control-allow-headers': 'Content-Type, Authorization',
        'access-control-allow-methods': 'POST, OPTIONS',
        'access-control-max-age': '600',
    }, headers);
};


