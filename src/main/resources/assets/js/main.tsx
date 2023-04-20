import {GraphiQL} from 'graphiql';
import {createGraphiQLFetcher} from '@graphiql/toolkit';
import * as React from 'react';
import {useState} from 'react';
import * as ReactDOM from 'react-dom';
import {Button, ButtonGroup} from '@graphiql/react';
import {createClient} from 'graphql-ws';

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

let currentBranch = 'draft';

function getRootContainer() {
    return document.getElementById('graphiql-container');
}

function getHandlerUrl() {
    return `${getRootContainer().dataset.configHandlerUrl}/${getProjectValue()}/${currentBranch}`;
}

function getWsHandlerUrl() {
    return `${getRootContainer().dataset.configWsUrl}/${getProjectValue()}/${currentBranch}`;
}

function getProjectValue(): string {
    return window['libAdmin'].store.get('projectContext').currentProject.name;
}

function renderGraphiQLUI() {
    ReactDOM.render(<QueryPlayground/>, getRootContainer(), function () {
        const refreshButton = document.querySelector('[aria-label="Re-fetch GraphQL schema"]');
        refreshButton.addEventListener('click', rerenderGraphiQLUI);

        const bodyElement = document.querySelector('body');
        bodyElement.classList.remove('graphiql-dark');
        bodyElement.classList.add('graphiql-light');

        const settingsButton = document.querySelector('[aria-label="Open settings dialog"]');
        settingsButton.addEventListener('click', function () {
            setTimeout(function () {
                const titleElements = document.querySelectorAll('.graphiql-dialog-section-title');
                titleElements.forEach(function (titleElement) {
                    if (titleElement.innerHTML === 'Theme') {
                        titleElement.closest('.graphiql-dialog-section').remove();
                    }
                });
            }, 1);
        });
    });
}

function rerenderGraphiQLUI() {
    ReactDOM.unmountComponentAtNode(getRootContainer());
    renderGraphiQLUI();
}

function initEventListeners() {
    window['libAdmin'].store.get('projectContext').onProjectChanged(function () {
        rerenderGraphiQLUI();
    });
}

function createFetcher() {
    return createGraphiQLFetcher({
        url: getHandlerUrl(),
        wsClient: createClient(
            {
                url: getWsHandlerUrl(),
            }),
    });
}

function BranchChooser() {
    const [branch, setBranch] = useState(currentBranch);

    const handleOnClick = (event, newBranch) => {
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

function QueryPlayground() {
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

initEventListeners();
renderGraphiQLUI();
