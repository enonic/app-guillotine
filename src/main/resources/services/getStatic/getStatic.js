const staticLib = require('/lib/enonic/static');
const router = require('/lib/router')();
router.all('{path:.*}', (request) => {
    return staticLib.requestHandler(
        request,
        {
            cacheControl: () => staticLib.RESPONSE_CACHE_CONTROL.SAFE,
            index: false,
            root: 'assets',
        }
    );
});

exports.all = function (req) {
    return router.dispatch(req);
};
