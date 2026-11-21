const contextLib = require('/lib/xp/context');
const contentLib = require('/lib/xp/content');
const projectLib = require('/lib/xp/project');
const authLib = require('/lib/xp/auth');
const ioLib = require('/lib/xp/io');

const LOG_PREFIX = '[mcp-dataset] ';
const dataset = JSON.parse(ioLib.readText(ioLib.getResource('/dataset/dataset.json').getStream()));
const PROJECT = dataset.project.id;
const REPO = 'com.enonic.cms.' + PROJECT;
const APP = dataset.app;

function asAdmin(branch, fn) {
    return contextLib.run({
        repository: REPO,
        branch: branch || 'draft',
        user: {login: 'su', idProvider: 'system'},
        principals: ['role:system.admin']
    }, fn);
}

function asUser(login, fn) {
    return contextLib.run({repository: REPO, branch: 'draft', user: {login: login, idProvider: 'system'}}, fn);
}

function userKey(login) {
    return 'user:system:' + login;
}

function ensureUsers() {
    dataset.users.forEach(function (u) {
        const key = userKey(u.login);
        if (!authLib.getPrincipal(key)) {
            authLib.createUser({idProvider: 'system', name: u.login, displayName: u.displayName, email: u.email});
            log.info(LOG_PREFIX + 'created user ' + key);
        }
        // Local-only test passwords; override with password.<login>=... in $XP_HOME/config/com.example.mcpdataset.cfg.
        authLib.changePassword({userKey: key, password: (app.config || {})['password.' + u.login] || u.password});
    });
}

function ensureProject() {
    const editors = dataset.users.map(function (u) { return userKey(u.login); });
    if (!projectLib.get({id: PROJECT})) {
        projectLib.create({
            id: PROJECT,
            displayName: dataset.project.displayName,
            language: dataset.project.language,
            permissions: {editor: editors},
            // ACL is copied to both branches; draft stays closed to anonymous because ContentService
            // requires a project role (or admin) for any read on the draft branch.
            publicRead: true
        });
        log.info(LOG_PREFIX + 'created project ' + PROJECT);
    } else {
        projectLib.addPermissions({id: PROJECT, permissions: {editor: editors}});
        if (!projectLib.get({id: PROJECT}).publicRead) {
            projectLib.setPublicRead({id: PROJECT, publicRead: true});
            log.info(LOG_PREFIX + 'set publicRead on ' + PROJECT);
        }
    }
}

function path() {
    return '/' + Array.prototype.slice.call(arguments).join('/');
}

function existing(p) {
    return contentLib.get({key: p});
}

function idOf(p) {
    const c = asAdmin('draft', function () { return existing(p); });
    if (!c) {
        throw new Error('Missing content ' + p);
    }
    return c._id;
}

function createIfMissing(login, p, createFn) {
    const found = asAdmin('draft', function () { return existing(p); });
    if (found) {
        return {content: found, created: false};
    }
    const created = asUser(login, createFn);
    log.info(LOG_PREFIX + 'created ' + p + ' as ' + login);
    return {content: created, created: true};
}

function ensureFolders() {
    dataset.folders.forEach(function (f) {
        createIfMissing('su', path(f.name), function () {
            return contentLib.create({
                parentPath: '/', name: f.name, displayName: f.displayName, contentType: 'base:folder', data: {}
            });
        });
    });
}

function ensurePersons() {
    dataset.persons.forEach(function (p) {
        createIfMissing(p.user, path('persons', p.name), function () {
            return contentLib.create({
                parentPath: '/persons', name: p.name, displayName: p.displayName,
                contentType: APP + ':person', data: {name: p.displayName}
            });
        });
    });
}

function ensureImages() {
    dataset.images.forEach(function (img) {
        createIfMissing(img.user, path('images', img.name), function () {
            return contentLib.createMedia({
                parentPath: '/images', name: img.name,
                data: ioLib.getResource('/dataset/images/' + img.file).getStream()
            });
        });
    });
}

function imgTag(id, alt) {
    return '<figure><img alt="' + alt + '" src="image://' + id + '"></figure>';
}

function articleData(a) {
    const data = {
        title: a.title,
        author: idOf(path('persons', a.author)),
        tags: a.tags,
        body: '<p>Body of ' + a.title + '.</p>' + (a.bodyImages || []).map(function (n) {
            return imgTag(idOf(path('images', n)), n);
        }).join('')
    };
    if (a.cover) {
        data.cover = idOf(path('images', a.cover));
    }
    return data;
}

function ensureArticles() {
    dataset.articles.forEach(function (a) {
        const data = articleData(a);
        createIfMissing(a.user, path('articles', a.name), function () {
            return contentLib.create({
                parentPath: '/articles', name: a.name, displayName: a.title,
                contentType: APP + ':article', data: data,
                workflow: {state: a.workflow, checks: {}}
            });
        });
    });
}

function ensureLandings() {
    dataset.landings.forEach(function (l) {
        if (asAdmin('draft', function () { return existing(path('landings', l.name)); })) {
            // a run that stopped between creating the landing and deleting its temporary targets left them behind
            l.links.filter(function (link) { return link.deleted; }).forEach(function (link) {
                const spec = dataset.deletedContent.filter(function (d) { return d.name === link.target; })[0];
                const tmpPath = path(spec.parent, spec.name);
                if (asAdmin('draft', function () { return existing(tmpPath); })) {
                    asAdmin('draft', function () { contentLib.delete({key: tmpPath}); });
                    log.info(LOG_PREFIX + 'deleted leftover ' + tmpPath);
                }
            });
            return;
        }
        const parts = ['<h1>' + l.displayName + '</h1>'];
        const toDelete = [];
        l.links.forEach(function (link) {
            let targetId;
            if (link.deleted) {
                const spec = dataset.deletedContent.filter(function (d) { return d.name === link.target; })[0];
                const tmpPath = path(spec.parent, spec.name);
                const tmp = createIfMissing(spec.user, tmpPath, function () {
                    return contentLib.create({
                        parentPath: '/' + spec.parent, name: spec.name, displayName: spec.title,
                        contentType: APP + ':article',
                        data: {title: spec.title, author: idOf(path('persons', spec.author))}
                    });
                }).content;
                targetId = tmp._id;
                toDelete.push(tmpPath);
            } else {
                targetId = idOf(path('articles', link.target));
            }
            parts.push('<p><a href="content://' + targetId + '">Link to ' + link.target + '</a></p>');
        });
        (l.images || []).forEach(function (n) {
            parts.push(imgTag(idOf(path('images', n)), n));
        });
        (l.missingImages || []).forEach(function (m) {
            parts.push(imgTag(m.id, m.label));
        });
        createIfMissing(l.user, path('landings', l.name), function () {
            return contentLib.create({
                parentPath: '/landings', name: l.name, displayName: l.displayName,
                contentType: APP + ':landing', data: {body: parts.join('')}
            });
        });
        toDelete.forEach(function (p) {
            asAdmin('draft', function () { contentLib.delete({key: p}); });
            log.info(LOG_PREFIX + 'deleted ' + p);
        });
    });
}

// All declared modifications are replayed in dataset order whenever any of them is missing, so the
// modifiedTime order stays "all creates, then modifications in dataset order" even after a partial run.
function ensureModifications() {
    const modified = dataset.articles.filter(function (a) { return a.modifiedBy; });
    const current = modified.map(function (a) {
        return asAdmin('draft', function () { return existing(path('articles', a.name)); });
    });
    const inPlace = current.every(function (c, i) {
        return c.modifier === userKey(modified[i].modifiedBy) && (i === 0 || c.modifiedTime > current[i - 1].modifiedTime);
    });
    if (inPlace) {
        return;
    }
    modified.forEach(function (a) {
        const p = path('articles', a.name);
        const edited = a.title + ' (edited)';
        // an update that changes nothing keeps modifiedTime/modifier, so revert first on a replay
        const titles = asAdmin('draft', function () { return existing(p); }).data.title === edited ? [a.title, edited] : [edited];
        titles.forEach(function (title) {
            asUser(a.modifiedBy, function () {
                contentLib.update({
                    key: p,
                    editor: function (v) {
                        v.data.title = title;
                        return v;
                    }
                });
            });
        });
        log.info(LOG_PREFIX + 'modified ' + p + ' as ' + a.modifiedBy);
    });
}

// update() always resets workflow to IN_PROGRESS (UpdateContentCommand), so restore the declared state.
function ensureWorkflow() {
    dataset.articles.forEach(function (a) {
        const p = path('articles', a.name);
        const c = asAdmin('draft', function () { return existing(p); });
        if (c.workflow && c.workflow.state === a.workflow) {
            return;
        }
        asUser(a.modifiedBy || a.user, function () {
            contentLib.updateWorkflow({
                key: p,
                editor: function (w) {
                    w.state = a.workflow;
                    return w;
                }
            });
        });
        log.info(LOG_PREFIX + 'workflow of ' + p + ' set to ' + a.workflow);
    });
}

function publishPaths() {
    const paths = [];
    dataset.folders.forEach(function (f) { if (f.publish) { paths.push(path(f.name)); } });
    dataset.persons.forEach(function (p) { if (p.publish) { paths.push(path('persons', p.name)); } });
    dataset.images.forEach(function (i) { if (i.publish) { paths.push(path('images', i.name)); } });
    dataset.articles.forEach(function (a) { if (a.publish) { paths.push(path('articles', a.name)); } });
    dataset.landings.forEach(function (l) { if (l.publish) { paths.push(path('landings', l.name)); } });
    return paths;
}

function ensurePublished() {
    const missing = publishPaths().filter(function (p) {
        return !asAdmin('master', function () { return contentLib.exists({key: p}); });
    });
    if (missing.length === 0) {
        return;
    }
    const keys = missing.map(function (p) { return idOf(p); });
    const result = asAdmin('draft', function () {
        // publishing a folder pushes its whole subtree unless its descendants are excluded
        return contentLib.publish({
            keys: keys,
            excludeDescendantsOf: dataset.folders.map(function (f) { return idOf(path(f.name)); }),
            includeDependencies: false,
            message: 'mcp dataset'
        });
    });
    log.info(LOG_PREFIX + 'published ' + result.pushedContents.length + ' of ' + keys.length + ', failed ' +
             JSON.stringify(result.failed.map(function (f) { return f.id + ':' + f.reason; })));
}

function countByType(branch) {
    const result = asAdmin(branch, function () {
        return contentLib.query({count: 0, aggregations: {byType: {terms: {field: 'type', size: 20}}}});
    });
    const byType = {};
    result.aggregations.byType.buckets.forEach(function (b) { byType[b.key] = b.docCount; });
    return {total: result.total, byType: byType};
}

function selfCheck() {
    const inProgress = asAdmin('draft', function () {
        return contentLib.query({
            count: 10,
            query: {term: {field: 'workflow.state', value: 'IN_PROGRESS'}}
        });
    });
    dataset.users.forEach(function (u) {
        const hits = asAdmin('draft', function () {
            return contentLib.query({
                count: 5, query: {term: {field: 'modifier', value: userKey(u.login)}}, sort: 'modifiedTime DESC'
            }).hits;
        });
        log.info(LOG_PREFIX + 'self-check: latest by ' + u.login + ': ' +
                 hits.map(function (h) { return h._path; }).join(', '));
    });
    log.info(LOG_PREFIX + 'self-check: draft term workflow.state=IN_PROGRESS total=' + inProgress.total + ' ' +
             JSON.stringify(inProgress.hits.map(function (h) { return h._path; })));
    log.info(LOG_PREFIX + 'self-check: draft ' + JSON.stringify(countByType('draft')));
    log.info(LOG_PREFIX + 'self-check: master ' + JSON.stringify(countByType('master')));
}

function run() {
    const started = Date.now();
    asAdmin('draft', function () {
        ensureUsers();
        ensureProject();
    });
    ensureFolders();
    ensurePersons();
    ensureImages();
    ensureArticles();
    ensureLandings();
    ensureModifications();
    ensureWorkflow();
    ensurePublished();
    selfCheck();
    log.info(LOG_PREFIX + 'done in ' + (Date.now() - started) + ' ms');
}

try {
    run();
} catch (e) {
    log.error(LOG_PREFIX + 'failed: ' + e + (e.stack ? '\n' + e.stack : ''));
}
