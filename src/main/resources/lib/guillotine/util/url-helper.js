const portalLib = require('/lib/xp/portal');
const contextUtilLib = require('/lib/guillotine/util/context-util');
const urlLib = require('/lib/urls');

exports.resolveAttachmentUrlById = function (env) {
    return contextUtilLib.executeInQueryContext(env.source['__searchTarget'], () => {
        return urlLib.attachmentUrl({
            id: env.source._id,
            download: env.args.download,
            type: env.args.type,
            params: env.args.params && JSON.parse(env.args.params)
        });
    });
}

exports.resolveAttachmentUrlByName = function (env) {
    return contextUtilLib.executeInQueryContext(env.source['__searchTarget'], () => {
        return urlLib.attachmentUrl({
            id: env.source['__nodeId'],
            name: env.source.name,
            download: env.args.download,
            type: env.args.type,
            params: env.args.params && JSON.parse(env.args.params)
        });
    });
}

exports.resolveImageUrl = function (env) {
    return contextUtilLib.executeInQueryContext(env.source['__searchTarget'], () => {
        return urlLib.imageUrl({
            id: env.source._id,
            scale: env.args.scale,
            quality: env.args.quality,
            background: env.args.background,
            format: env.args.format,
            filter: env.args.filter,
            type: env.args.type,
            params: env.args.params && JSON.parse(env.args.params)
        });
    });
}

exports.resolvePageUrl = function (env) {
    return contextUtilLib.executeInQueryContext(env.source['__searchTarget'], () => {
        return portalLib.pageUrl({
            id: env.source._id,
            type: env.args.type,
            params: env.args.params && JSON.parse(env.args.params)
        });
    });
}
