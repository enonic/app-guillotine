const portalLib = require('/lib/xp/portal');
const mustache = require('/lib/mustache');
const repoLib = require('/lib/xp/repo');

exports.get = function (req) {
    const view = resolve('guillotine.html');
    const assetsUrl = portalLib.assetUrl({path: ""});
    const repositories = getProjectRepositories();

    const params = {
        assetsUrl: assetsUrl,
        repositories: repositories
    };
    return {
        body: mustache.render(view, params),
        contentType: 'text/html'
    };
};

function getProjectRepositories() {
    let repositories = [{
        id: 'com.enonic.cms.default',
        displayName: 'Default'
    }];
    repoLib.list()
        .filter(repo => repo.id.startsWith('com.enonic.cms.') && repo.id !== 'com.enonic.cms.default')
        .forEach(repo => {
            repositories.push({
                id: repo.id,
                displayName: repo.data['com-enonic-cms'].displayName
            });
        });
    return repositories;
}
