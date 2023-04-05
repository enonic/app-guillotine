import {GraphiQL} from 'graphiql';
import {createGraphiQLFetcher} from '@graphiql/toolkit';
import * as React from 'react';
import {useState} from 'react';
import * as ReactDOM from 'react-dom';
import {createClient} from 'graphql-ws';
import {Button, ButtonGroup} from '@graphiql/react';

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

let currentBranch = 'master';

function getRootContainer() {
    return document.getElementById('graphiql-container-wrapper');
}

function getDataConfig() {
    return getRootContainer().dataset;
}

function createFetcher() {
    return createGraphiQLFetcher({
        url: `${getDataConfig().configHandlerUrl}/${currentBranch}`,
        wsClient: createClient(
            {
                url: `${getRootContainer().dataset.configWsUrl}/${currentBranch}`,
            }),
    });
}

function renderGraphiQLUI() {
    ReactDOM.render(<QueryPlayground/>, getRootContainer(), function () {
        const refreshButton = document.querySelector('[aria-label="Re-fetch GraphQL schema"]');
        refreshButton.addEventListener('click', rerenderGraphiQLUI);
    });
}

function rerenderGraphiQLUI() {
    ReactDOM.unmountComponentAtNode(getRootContainer());
    renderGraphiQLUI();
}

function BranchChooser() {
    const [branch, setBranch] = useState(currentBranch);

    const handleOnClick = (event, selectedBranch) => {
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

renderGraphiQLUI();
