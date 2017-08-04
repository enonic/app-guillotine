var authLib = require('/lib/xp/auth');

exports.isAdmin = function () {
    log.info('isAdmin:' + authLib.hasRole('system.admin'));
    return authLib.hasRole('system.admin');
};
exports.isCmsAdmin = function () {
    log.info('isCmsAdmin:' + authLib.hasRole('cms.admin'));
    return authLib.hasRole('cms.admin');
};