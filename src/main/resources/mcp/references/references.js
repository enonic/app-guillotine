/* global __ */

const tools = __.newBean('com.enonic.app.guillotine.mcp.McpToolsBean');

exports.execute = function (name, args) {
    return JSON.parse(tools.run('references', name, JSON.stringify(args || {})));
};
