exports.getInstalledApplications = function () {
    const bean = __.newBean('com.enonic.app.guillotine.ApplicationHandler');
    return __.toNativeObject(bean.getInstalledApplicationKeys());
}
