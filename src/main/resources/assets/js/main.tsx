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
    return `${getRootContainer().dataset.configHandlerUrl}?project=${getCurrentProjectValue()}&branch=${currentBranch}`;
}

function getCurrentProjectValue(): string {
    const key = 'enonic:cs:lastselectedprojectid';
    const projectValue = localStorage.getItem(key);
    if (projectValue === null) {
        throw new Error(`Missing localStorage value for "${key}"`);
    }
    return JSON.parse(projectValue);
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
    window.addEventListener('enonic:cs:active-project-changed', rerenderGraphiQLUI);
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
    });
}


function QueryPlayground() {
    useEffect(() => {
        renderCallback();
    }, []);

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
