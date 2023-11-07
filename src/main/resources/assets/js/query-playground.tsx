import {GraphiQL} from 'graphiql';
import {createGraphiQLFetcher} from '@graphiql/toolkit';
import * as React from 'react';
import * as ReactDOM from 'react-dom';
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

function getRootContainer() {
    return document.getElementById('graphiql-container-wrapper');
}

function getDataConfig() {
    return getRootContainer().dataset;
}

function createFetcher() {
    return createGraphiQLFetcher({
        url: `${getDataConfig().configHandlerUrl}`,
        wsClient: createClient(
            {
                url: `${getRootContainer().dataset.configWsUrl}`,
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

function QueryPlayground() {
    return (
        <GraphiQL fetcher={createFetcher()}
                  defaultQuery={DEFAULT_QUERY}
        />
    );
}

renderGraphiQLUI();
