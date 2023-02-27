/* global __ */
const contextUtilLib = require('/lib/guillotine/util/context-util');

exports.processHtml = function (params, searchTarget) {
    const bean = __.newBean('com.enonic.app.guillotine.handler.ProcessHtmlHandler');
    return contextUtilLib.executeInQueryContext(searchTarget, () => __.toNativeObject(bean.processHtml(__.toScriptValue(params))));
};
