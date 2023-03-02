const portalLib = require('/lib/xp/portal');
const contextUtilLib = require('/lib/guillotine/util/context-util');
const urlLib = require('/lib/urls');

function wrapUrl(url, urlType, searchTarget) {
    const project = searchTarget.repository.replace('com.enonic.cms.', '');
    const branch = searchTarget.branch;

    let position = url.indexOf('/_/');

    if (urlType === 'absolute') {
        if (position === -1) { // content or page URL
            position = url.indexOf("/", url.indexOf("://") + 3);
        }
        return `${url.substring(0, position)}/site/${project}/${branch}/${url.substring(position + 1)}`;
    } else { // urlType === server
        return `/site/${project}/${branch}${url}`;
    }
}

function wrapImageUrl(url, urlType, searchTarget) {
    const project = searchTarget.repository.replace('com.enonic.cms.', '');
    const branch = searchTarget.branch;

    let position = url.indexOf('/_/');

    if (urlType === 'absolute') {
        if (position === -1) { // content or page URL
            position = url.indexOf("/", url.indexOf("://") + 3);
        }
        return `${url.substring(0, position)}/api/_/image/${project}/${branch}/${url.substring(position + '/_/image/'.length)}`;
    } else { // urlType === server
        return `/api/_/image/${project}/${branch}/${url.replace('/_/image/', '')}`;
    }
}

exports.resolveAttachmentUrlById = function (env) {
    return contextUtilLib.executeInQueryContext(env.source['__searchTarget'], () => {
        return wrapUrl(portalLib.attachmentUrl({
            id: env.source._id,
            download: env.args.download,
            type: env.args.type,
            params: env.args.params && JSON.parse(env.args.params)
        }), env.args.type, env.source['__searchTarget']);
    });
}

exports.resolveAttachmentUrlByName = function (env) {
    return contextUtilLib.executeInQueryContext(env.source['__searchTarget'], () => {
        return wrapUrl(portalLib.attachmentUrl({
            id: env.source['__nodeId'],
            name: env.source.name,
            download: env.args.download,
            type: env.args.type,
            params: env.args.params && JSON.parse(env.args.params)
        }), env.args.type, env.source['__searchTarget']);
    });
}

exports.resolveImageUrl = function (env) {
    return contextUtilLib.executeInQueryContext(env.source['__searchTarget'], () => {
        return urlLib.imageUrl({
            id: env.source._id,
            scale: env.args.scale,
            type: env.args.type
        });
    });
    // return contextUtilLib.executeInQueryContext(env.source['__searchTarget'], () => {
    //     return wrapImageUrl(portalLib.imageUrl({
    //         id: env.source._id,
    //         scale: env.args.scale,
    //         quality: env.args.quality,
    //         background: env.args.background,
    //         format: env.args.format,
    //         filter: env.args.filter,
    //         type: env.args.type,
    //         params: env.args.params && JSON.parse(env.args.params)
    //     }), env.args.type, env.source['__searchTarget']);
    // });
}

exports.resolvePageUrl = function (env) {
    return contextUtilLib.executeInQueryContext(env.source['__searchTarget'], () => {
        return wrapUrl(portalLib.pageUrl({
            id: env.source._id,
            type: env.args.type,
            params: env.args.params && JSON.parse(env.args.params)
        }), env.args.type, env.source['__searchTarget']);
    });
}
