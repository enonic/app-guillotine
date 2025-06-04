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

let currentBranch: string = 'master';

function getRootContainer(): HTMLElement {
    return document.getElementById('graphiql-container-wrapper');
}

function getDataConfig() {
    return getRootContainer().dataset;
}

function createFetcher() {
    return createGraphiQLFetcher({
        url: `${getDataConfig().configHandlerUrl}?branch=${currentBranch}`,
    });
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

function BranchChooser() {
    const [branch, setBranch] = useState<string>(currentBranch);

    const handleOnClick = (event, selectedBranch: string) => {
        currentBranch = selectedBranch;
        setBranch(selectedBranch);

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

function renderCallback() {
    requestAnimationFrame(() => {
        const refreshButton: Element = document.querySelector('[aria-label="Re-fetch GraphQL schema"]');
        refreshButton?.removeEventListener('click', rerenderGraphiQLUI);
        refreshButton?.addEventListener('click', rerenderGraphiQLUI);
    });
}

function QueryPlayground() {
    useEffect(() => {
        renderCallback();
    });

    return (
        <GraphiQL fetcher={createFetcher()}
                  defaultQuery={DEFAULT_QUERY}
        >
            <GraphiQL.Logo>
                <BranchChooser/>
            </GraphiQL.Logo>
        </GraphiQL>
    );
}

renderGraphiQLUI();
