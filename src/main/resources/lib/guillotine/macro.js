/* global __ */

exports.processHtml = function (params) {
    let bean = __.newBean('com.enonic.app.guillotine.handler.ProcessHtmlHandler');
    return __.toNativeObject(bean.processHtml(__.toScriptValue(params)));
};
