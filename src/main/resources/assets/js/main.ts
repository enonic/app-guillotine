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
let branch = 'draft';

function getHandlerUrl() {
    return `${getRootContainer().dataset.configHandlerUrl}/${getProjectValue()}/${branch}`;
}

function getProjectValue(): string {
    return window['libAdmin'].store.get('projectContext').currentProject.name;
}

function initEventListeners() {
    window['libAdmin'].store.get('projectContext').onProjectChanged(function () {
        toggleGraphiQLEditor();
    });
}

function toggleGraphiQLEditor() {
    if (activeSocket != null) {
        const executeBtn = document.querySelector('.app-guillotine-container .execute-button');
        (<HTMLElement>executeBtn).click();
    }

    if (root !== null) {
        root.unmount();
    }
    renderGraphiQLUI();
}

function renderGraphiQLUI() {
    root = createRoot(getRootContainer());
    root.render(createMainElement());
}

function createMainElement() {
    return React.createElement(GraphiQL, {
        fetcher: createFetcher(),
        defaultVariableEditorOpen: false,
        children: [createLogoReplacement()],
        toolbar: {
            additionalContent: createRefreshButton()
        },
        defaultQuery: DEFAULT_QUERY,
    });
}

function createFetcher() {
    return createGraphiQLFetcher({
        url: getHandlerUrl(),
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
    return `${getRootContainer().dataset.configWsUrl}/${getProjectValue()}/${branch}`;
}

function getRootContainer() {
    return document.getElementById('graphiql-container');
}

function createBranchMenu() {
    const draftMenuItem = React.createElement(GraphiQL.MenuItem, {
        key: 'draft',
        label: 'draft',
        onSelect: () => {
            branch = 'draft';
            toggleGraphiQLEditor();
        }
    });

    const masterMenuItem = React.createElement(GraphiQL.MenuItem, {
        key: 'master',
        label: 'master',
        onSelect: () => {
            branch = 'master';
            toggleGraphiQLEditor();
        }
    });

    const branchMenu = React.createElement(GraphiQL.Menu, {
        key: 'branch',
        label: `Branch (${branch})`,
        children: [draftMenuItem, masterMenuItem]
    });

    return branchMenu;
}

function createLogoReplacement() {
    return React.createElement(GraphiQL.Logo, {
        key: 'logo',
        children: [createBranchMenu()]
    });
}

function createRefreshButton() {
    return React.createElement(GraphiQL.Button, {
        key: 'refreshButton',
        title: 'Refresh',
        onClick: () => {
            toggleGraphiQLEditor();
        }
    });
}

initEventListeners();
renderGraphiQLUI();
