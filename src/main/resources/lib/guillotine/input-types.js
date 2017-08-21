var graphQlLib = require('/lib/graphql');

exports.createProcessHtmlInputType = function (context) {
    return graphQlLib.createInputObjectType({
        name: context.uniqueName('ProcessHtmlInputType'),
        description: 'Process HTML input type.',
        fields: {
            'type': {
                type: createUrlTypeType(context)
            }
        }
    })
};

function createUrlTypeType(context) {
    return graphQlLib.createEnumType({
        name: context.uniqueName('UrlTypeType'),
        description: 'URL type type.',
        values: {
            'server': 'server',
            'absolute': 'absolute'
        }
    });
}