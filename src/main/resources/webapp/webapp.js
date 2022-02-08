const guillotineLib = require('/lib/guillotine');
const appLib = require('/lib/app');
const contextLib = require('/lib/xp/context');
const router = require('/lib/router')();

exports.all = function (req) {
    return router.dispatch(req);
};

const SCHEMA = guillotineLib.createSchema({
    applications: appLib.getInstalledApplications().applications,
    creationCallbacks: {
        'HeadlessCms': function (context, params) {
            delete params.fields.getSite;
        }
    },
    mode: 'project'
});

router.post('/{project}/{branch}', function (req) {
    let input = JSON.parse(req.body);

    let params = {
        query: input.query,
        variables: input.variables,
        schema: SCHEMA
    };
    return {
        contentType: 'application/json',
        body: contextLib.run({
            repository: req.pathParams.project,
            branch: req.pathParams.branch
        }, function () {
            return guillotineLib.execute(params)
        })
    };
});
