/* global __ */

// Loading the schema library registers the listener that rebuilds the schema when applications change.
require('/lib/schema');

const mcpTools = __.newBean('com.enonic.app.guillotine.mcp.McpToolsBean');

// On Nashorn args is a java.util.Map, which JSON.stringify turns into undefined; the bean then reads the map itself.
exports.execute = function (name, args) {
    const json = JSON.stringify(args || {});
    return JSON.parse(mcpTools.run(name, json !== undefined ? json : __.toScriptValue(args)));
};
