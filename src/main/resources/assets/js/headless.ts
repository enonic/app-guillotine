import * as React from 'react';
import {createRoot} from 'react-dom/client';
import {GraphiQL} from 'graphiql';
import {createGraphiQLFetcher} from '@graphiql/toolkit';
import {createClient} from 'graphql-ws';

const DEFAULT_QUERY = `# Welcome to Query Playground
#
# Query Playground is an in-browser tool for writing, validating, and
# testing GraphQL queries.
#
# An example GraphQL query might look like this:
#
#     {
#       guillotine {
#         getChildren(key: "/") {
#           displayName
#         }
#       }
#     }
#
#
`;

let root = null;
let activeSocket = null;


function renderGraphiQLUI() {
    root = createRoot(getRootContainer());
    root.render(createMainElement());
}

function createMainElement() {
    return React.createElement(GraphiQL, {
        fetcher: createFetcher(),
        defaultVariableEditorOpen: false,
        defaultQuery: DEFAULT_QUERY,
    });
}

function createFetcher() {
    return createGraphiQLFetcher({
        url: getRootContainer().dataset.configHandlerUrl,
        wsClient: createWsClient(),
    });
}

function createWsClient() {
    return createClient(
        {
            url: getClientEndpoint,
            lazy: true,
            on: {
                opened: (socket) => {
                    activeSocket = socket;
                }
            }
        });
}

function getClientEndpoint() {
    return `${getRootContainer().dataset.configWsUrl}`;
}

function getRootContainer() {
    return document.getElementById('graphiql-container');
}

renderGraphiQLUI();
