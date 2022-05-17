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
        activeSocket.close(1000);
    }

    root.unmount();
    renderGraphiQLUI();
}

function renderGraphiQLUI() {
    const clientEndpoint = `ws://${window.location.host}${getHandlerUrl()}`;

    const wsClient = createClient(
        {
            url: clientEndpoint,
            retryAttempts: 0,
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

    const container = document.getElementById('graphiql-container');
    root = createRoot(container);
    root.render(element);
}

initEventListeners();
renderGraphiQLUI();
