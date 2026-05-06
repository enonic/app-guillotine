exports.resolveHeaders = function (config, req) {
    if (config['cors.enabled'] === 'false') {
        return {};
    }

    const headers = {};

    if (config['cors.origin']) {
        headers['access-control-allow-origin'] = config['cors.origin'];
        headers['vary'] = 'Origin';
    } else if (req.getHeader('Origin')) {
        headers['access-control-allow-origin'] = req.getHeader('Origin');
        headers['vary'] = 'Origin';
    } else {
        headers['access-control-allow-origin'] = '*';
    }

    if ((config['cors.credentials'] || '') === 'true') {
        headers['access-control-allow-credentials'] = 'true';
    }

    headers['access-control-allow-headers'] = config['cors.allowedHeaders'] || 'Content-Type';
    headers['access-control-allow-methods'] = config['cors.methods'] || 'POST, OPTIONS';

    if (config['cors.maxAge']) {
        headers['access-control-max-age'] = config['cors.maxAge'];
    }

    return headers;
};


exports.getHeaders = function (req) {
    return exports.resolveHeaders(app.config, req);
};
