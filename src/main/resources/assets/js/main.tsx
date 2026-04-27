import {GraphiQL} from 'graphiql';
import {createGraphiQLFetcher} from '@graphiql/toolkit';
import * as React from 'react';
import {useEffect, useState} from 'react';
import {createRoot} from 'react-dom/client';
import {Button, ButtonGroup} from '@graphiql/react';

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

let currentBranch: string = 'draft';

function getRootContainer(): HTMLElement {
    return document.getElementById('graphiql-container');
}

function getHandlerUrl(): string {
    return `${getRootContainer().dataset.configHandlerUrl}?project=${getProjectValue()}&branch=${currentBranch}`;
}

function getProjectValue(): string {
    return localStorage.getItem('contentstudio:defaultProject');
}

let root: ReturnType<typeof createRoot> | null = null;

function renderGraphiQLUI() {
    const container = getRootContainer();

    if (!root) {
        root = createRoot(container);
    }

    root.render(<QueryPlayground/>);
}

function rerenderGraphiQLUI() {
    if (root) {
        root.unmount();
        root = null;
    }
    renderGraphiQLUI();
}

function initEventListeners() {
    let currentProjectName = localStorage.getItem('contentstudio:defaultProject');

    setInterval(() => {
        const newProjectName = localStorage.getItem('contentstudio:defaultProject');
        if (newProjectName !== currentProjectName) {
            currentProjectName = newProjectName;
            rerenderGraphiQLUI();
        }
    }, 500);
}

function createFetcher() {
    return createGraphiQLFetcher({
        url: getHandlerUrl(),
    });
}

function BranchChooser() {
    const [branch, setBranch] = useState<string>(currentBranch);

    const handleOnClick = (event, newBranch: string) => {
        currentBranch = newBranch;
        setBranch(newBranch);

        rerenderGraphiQLUI();
    };

    return (
        <ButtonGroup>
            <Button
                type="button"
                className={branch === 'draft' ? 'active' : ''}
                onClick={(event) => handleOnClick(event, 'draft')}
            >
                Draft
            </Button>
            <Button
                type="button"
                className={branch === 'master' ? 'active' : ''}
                onClick={(event) => handleOnClick(event, 'master')}
            >
                Master
            </Button>
        </ButtonGroup>
    );
}

function settingButtonCallback() {
    setTimeout(() => {
        const titleElements: NodeListOf<Element> = document.querySelectorAll('.graphiql-dialog-section-title');
        titleElements.forEach((titleElement: Element) => {
            if (titleElement.textContent === 'Theme') {
                titleElement.closest('.graphiql-dialog-section')?.remove();
            }
        });
    }, 1);
}

function setupButtonEvent(selector: string, handler: () => void) {
    const button: Element | null = document.querySelector(selector);
    if (button) {
        button.removeEventListener('click', handler);
        button.addEventListener('click', handler);
    }
}

function renderCallback() {
    requestAnimationFrame(() => {
        setupButtonEvent('[aria-label="Re-fetch GraphQL schema"]', rerenderGraphiQLUI);

        document.body.classList.remove('graphiql-dark');
        document.body.classList.add('graphiql-light');

        setupButtonEvent('[aria-label="Open settings dialog"]', settingButtonCallback);
    });
}


function QueryPlayground() {
    useEffect(() => {
        renderCallback();
    });

    return (
        <GraphiQL
            fetcher={createFetcher()}
            defaultQuery={DEFAULT_QUERY}
        >
            <GraphiQL.Logo>
                <BranchChooser/>
            </GraphiQL.Logo>
        </GraphiQL>
    );
}

initEventListeners();
renderGraphiQLUI();
