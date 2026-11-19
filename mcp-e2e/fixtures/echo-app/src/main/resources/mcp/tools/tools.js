const contextLib = require('/lib/xp/context');

exports.execute = function (name, args) {
    if (name === 'fail') {
        throw new Error('Echo fixture failure');
    }
    if (name === 'args-probe') {
        const json = JSON.stringify(args);
        return {content: [{type: 'text', text: json === undefined ? 'stringify:undefined' : 'stringify:' + json}]};
    }
    const user = contextLib.get().authInfo.user;
    const result = {message: args.message};
    if (name === 'undefined-field') {
        // Probe: a naive controller leaves an optional field undefined.
        result.user = user ? user.key : undefined;
    } else if (user) {
        result.user = user.key;
    }
    return {
        content: [{type: 'text', text: JSON.stringify(result)}],
        structuredContent: result
    };
};
