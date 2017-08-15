var graphQlLib = require('/lib/graphql');
var namingLib = require('/lib/headless-cms/naming');

exports.urlTypeType = graphQlLib.createEnumType({
    name: namingLib.uniqueName('UrlTypeType'),
    description: 'URL type type.',
    values: {
        'server': 'server',
        'absolute': 'absolute'
    }
});

exports.processHtmlInputType = graphQlLib.createInputObjectType({
    name: namingLib.uniqueName('ProcessHtmlInputType'),
    description: 'Process HTML input type.',
    fields: {
        'type': {
            type: exports.urlTypeType
        }
    }
});