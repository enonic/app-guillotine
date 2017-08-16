var authLib = require('/lib/xp/auth');
var portalLib = require('/lib/xp/portal');

exports.isSiteContext = function() {
    return  !!portalLib.getSite();
};
exports.isAllowedSiteContext = function() {
    return  !!portalLib.getSiteConfig();
};
exports.isAdmin = function () {
    log.info('isAdmin:' + authLib.hasRole('system.admin'));
    return authLib.hasRole('system.admin');
};
exports.isCmsAdmin = function () {
    log.info('isCmsAdmin:' + authLib.hasRole('cms.admin'));
    return authLib.hasRole('cms.admin');
};