import {GraphiQL} from 'graphiql';
import {createGraphiQLFetcher} from '@graphiql/toolkit';
import {createClient} from 'graphql-ws';
import * as React from 'react';
import {createRoot} from 'react-dom/client';

const DEFAULT_QUERY = `# Welcome to Query Playground
#
# Query Playground is an in-browser tool for writing, validating, and
# testing GraphQL queries.
#
# An example GraphQL query might look like this:
query {
  guillotine {
    getChildren(key: "/") {
      displayName
    }
  }
}
`;

function getRootContainer(): HTMLElement {
    return document.getElementById('graphiql-container');
}

function getHandlerUrl(): string {
    return getRootContainer().dataset.configHandlerUrl;
}

function getWsHandlerUrl(): string {
    return `${getRootContainer().dataset.configWsUrl}`;
}

let root: ReturnType<typeof createRoot> | null = null;

function renderGraphiQLUI() {
    const container = getRootContainer();

    if (!root) {
        root = createRoot(container);
    }

    root.render(<QueryPlayground/>);

    // Wait for DOM updates
    setTimeout(() => {
        const refreshButton: Element = document.querySelector('[aria-label="Re-fetch GraphQL schema"]');
        refreshButton?.addEventListener('click', rerenderGraphiQLUI);

        document.body.classList.remove('graphiql-dark');
        document.body.classList.add('graphiql-light');

        const settingsButton: Element = document.querySelector('[aria-label="Open settings dialog"]');
        settingsButton?.addEventListener('click', () => {
            setTimeout(() => {
                const titleElements: NodeListOf<Element> = document.querySelectorAll('.graphiql-dialog-section-title');
                titleElements.forEach((titleElement: Element) => {
                    if (titleElement.textContent === 'Theme') {
                        titleElement.closest('.graphiql-dialog-section')?.remove();
                    }
                });
            }, 1);
        });
    }, 0);
}

function rerenderGraphiQLUI() {
    if (root) {
        root.unmount();
        root = null;
    }
    renderGraphiQLUI();
}

function initEventListeners() {
    window['libAdmin'].store.get('projectContext').onProjectChanged(() => {
        rerenderGraphiQLUI();
    });
}

function createFetcher() {
    return createGraphiQLFetcher({
        url: getHandlerUrl(),
        wsClient: createClient({
            url: getWsHandlerUrl(),
        }),
    });
}

function QueryPlayground() {
    return (
        <GraphiQL
            fetcher={createFetcher()}
            defaultQuery={DEFAULT_QUERY}
        />
    );
}

initEventListeners();
renderGraphiQLUI();
