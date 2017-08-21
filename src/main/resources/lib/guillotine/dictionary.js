var portalLib = require('/lib/xp/portal');

var dictionaryMap = {};

exports.get = function () {
    var siteId = portalLib.getSite()._id;
    return dictionaryMap[siteId];
};

exports.add = function (objectType) {
    var siteId = portalLib.getSite()._id;
    var dictionary = dictionaryMap[siteId];
    if (!dictionary) {
        dictionary = [];
        dictionaryMap[siteId] = dictionary;
    }
    dictionary.push(objectType);
};

exports.reset = function () {
    dictionaryMap = {};
};