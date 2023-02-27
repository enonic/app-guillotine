const contextLib = require('/lib/xp/context');

exports.executeInQueryContext = function (searchTarget, callback) {
    return contextLib.run({
        repository: searchTarget.repository,
        branch: searchTarget.branch,
    }, callback);
};
