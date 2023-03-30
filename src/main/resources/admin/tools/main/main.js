const portalLib = require('/lib/xp/portal');
const mustacheLib = require('/lib/mustache');
const repoLib = require('/lib/xp/repo');

exports.get = function (req) {
    const view = resolve('main.html');

    const assetsUrl = portalLib.assetUrl({path: ''});
    const baseUrl = '/admin/site/preview';
    const wsUrl = portalLib.url({
        path: baseUrl,
        type: 'websocket',
    });
    const handlerUrl = portalLib.url({
        path: baseUrl,
    });

    const projects = ['default'].concat(
        repoLib.list().filter(repo => repo.id.startsWith('com.enonic.cms.') && repo.id !== 'com.enonic.cms.default').map(repo => {
            return repo.id.replace('com.enonic.cms.', '');
        })).join(',');

    const params = {
        assetsUrl: assetsUrl,
        wsUrl: wsUrl,
        handlerUrl: handlerUrl,
        projects: projects,
        defaultProject: req.repositoryId ? req.repositoryId.replace('com.enonic.cms.', '') : 'default',
        defaultBranch: req.branch ? req.branch : 'draft',
    };

    return {
        status: 200,
        contentType: 'text/html',
        body: mustacheLib.render(view, params)
    };
};
