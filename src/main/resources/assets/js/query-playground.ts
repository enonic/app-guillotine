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

function getRootContainer() {
    return document.getElementById('graphiql-container-wrapper');
}

function getDataConfig() {
    return getRootContainer().dataset;
}

let root = null;
let activeSocket = null;
let branch = getDataConfig().defaultBranch;
let project = getDataConfig().defaultProject;

function getHandlerUrl() {
    return `${getDataConfig().configHandlerUrl}/${project}/${branch}`;
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
    return `${getDataConfig().configWsUrl}/${project}/${branch}`;
}

function getProjects() {
    return getDataConfig().repositories.split(',', -1);
}

function createProjectMenu() {
    const projects = [];
    getProjects().forEach(repo => {
        projects.push(React.createElement(GraphiQL.MenuItem, {
            key: repo,
            label: repo,
            onSelect: () => {
                project = repo;
                toggleGraphiQLEditor();
            }
        }));
    });

    return React.createElement(GraphiQL.Menu, {
        key: 'project',
        label: `Project (${project})`,
        children: projects
    });
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

    return React.createElement(GraphiQL.Menu, {
        key: 'branch',
        label: `Branch (${branch})`,
        children: [draftMenuItem, masterMenuItem]
    });
}

function createLogoReplacement() {
    return React.createElement(GraphiQL.Logo, {
        key: 'logo',
        children: [createProjectMenu(), createBranchMenu()]
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

renderGraphiQLUI();
