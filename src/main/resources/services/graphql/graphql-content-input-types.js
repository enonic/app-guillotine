var graphQlLib = require('/lib/graphql');

exports.urlTypeType = graphQlLib.createEnumType({
    name: 'UrlTypeType',
    description: 'URL type type.',
    values: {
        'server': 'server',
        'absolute': 'absolute'
    }
});

exports.processHtmlInputType = graphQlLib.createInputObjectType({
    name: 'ProcessHtmlInputType',
    description: 'Process HTML input type.',
    fields: {
        'type': {
            type: exports.urlTypeType
        }
    }
});