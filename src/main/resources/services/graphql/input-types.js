var graphQlLib = require('/lib/graphql');
var namingLib = require('/lib/headless-cms/naming');

exports.createProcessHtmlInputType = function () {
    return graphQlLib.createInputObjectType({
        name: namingLib.uniqueName('ProcessHtmlInputType'),
        description: 'Process HTML input type.',
        fields: {
            'type': {
                type: createUrlTypeType()
            }
        }
    })
};

function createUrlTypeType() {
    return graphQlLib.createEnumType({
        name: namingLib.uniqueName('UrlTypeType'),
        description: 'URL type type.',
        values: {
            'server': 'server',
            'absolute': 'absolute'
        }
    });
}