const bean = __.newBean('com.enonic.app.guillotine.handler.UrlHandler');

function checkRequired(obj, name) {
    if (obj == null || obj[name] === undefined) {
        throw `Parameter '${name}' is required`;
    }
}

exports.assetUrl = function (path, type) {
    return __.toNativeObject(bean.assetUrl(path, type));
};

exports.imageUrl = function (params) {
    checkRequired(params, 'id');
    checkRequired(params, 'scale');

    return __.toNativeObject(bean.imageUrl(__.toScriptValue(params)));
};

// http://localhost:8080/api/_/image/hmdb/draft/b0c9f9f1-1928-4a6a-9bfb-666559552a26:b090a1822f68634e5b9d11390aede8b43cf15088/block-310-175/20220910_121821.jpg
// http://api.myapp.com:8080/_/image/hmdb/draft/b0c9f9f1-1928-4a6a-9bfb-666559552a26:b090a1822f68634e5b9d11390aede8b43cf15088/block-310-175/20220910_121821.jpg
