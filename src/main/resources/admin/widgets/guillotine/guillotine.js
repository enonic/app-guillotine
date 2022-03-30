const mustache = require('/lib/mustache');

function handleGet() {
    const view = resolve('./guillotine.html');
    const params = {
    //
    };

    return {
        contentType: 'text/html',
        body: mustache.render(view, params),
    };
}

exports.get = handleGet;
