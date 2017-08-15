exports.generateCamelCase = function (text, upper) {
    var sanitizedText = exports.sanitizeText(text);
    var camelCasedText = sanitizedText.replace(/_[0-9A-Za-z]/g, function (match, offset, string) {
        return match.charAt(1).toUpperCase();
    });
    var firstCharacter = upper ? camelCasedText.charAt(0).toUpperCase() : camelCasedText.charAt(0).toLowerCase();
    return firstCharacter + (camelCasedText.length > 1 ? camelCasedText.substr(1) : '');
};

exports.sanitizeText = function (text) {
    return text.replace(/([^0-9A-Za-z])+/g, '_');
};


var nameSet = {};
exports.uniqueName = function (name) {
    if (nameSet[name]) {
        name = name + '_' + generateRandomString();
    }
    nameSet[name] = true;
    return name;
};
exports.resetNameSet = function () {
    nameSet = {};
};

function generateRandomString() {
    return Math.random().toString(36).substr(2, 10).toUpperCase() + Math.random().toString(36).substr(2, 6).toUpperCase();
};