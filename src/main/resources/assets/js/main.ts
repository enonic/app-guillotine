import * as React from 'react';
import {createRoot} from 'react-dom/client';
import {GraphiQL} from 'graphiql';
import {createGraphiQLFetcher} from '@graphiql/toolkit';
import {createClient} from 'graphql-ws';

let root = null;
let activeSocket = null;

function getHandlerUrl() {
    const projectVal: string = window['libAdmin'].store.get('projectContext').currentProject.name;
    const branchVal: string = (document.getElementById('branch') as HTMLInputElement).value;

    return `/admin/site/preview/${projectVal}/${branchVal}`;
}

function initEventListeners() {
    let elementById = document.getElementById('branch');
    elementById.removeEventListener('change', toggleGraphiQLEditor);
    elementById.addEventListener('change', toggleGraphiQLEditor);

    window['libAdmin'].store.get('projectContext').onProjectChanged(function () {
        toggleGraphiQLEditor();
    });
}

function toggleGraphiQLEditor() {
    if (activeSocket != null) {
        const executeBtn = document.querySelector('.app-guillotine-container .execute-button');
        (<HTMLElement>executeBtn).click();
    }

    root.unmount();
    renderGraphiQLUI();
}

function renderGraphiQLUI() {
    const container = document.getElementById('graphiql-container');

    const clientEndpoint = `${container.dataset.configWsUrl}${getHandlerUrl()}`;

    const wsClient = createClient(
        {
            url: clientEndpoint,
            lazy: true,
            on: {
                opened: (socket) => {
                    activeSocket = socket;
                }
            }
        });

    const fetcher = createGraphiQLFetcher({
        url: getHandlerUrl(),
        wsClient: wsClient
    });

    const element = React.createElement(GraphiQL, {
        fetcher: fetcher,
        defaultVariableEditorOpen: false
    });

    root = createRoot(container);
    root.render(element);
}

initEventListeners();
renderGraphiQLUI();
