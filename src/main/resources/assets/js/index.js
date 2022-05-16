function getHandlerUrl() {
    const projectVal = window.libAdmin.store.get('projectContext').currentProject.name;
    const branchVal = document.getElementById('branch').value;

    return `/admin/site/preview/${projectVal}/${branchVal}`;
}

waitFor('GraphiQL', function () {
    let elementById = document.getElementById('branch');
    elementById.removeEventListener('change', toggleGraphiQLEditor);
    elementById.addEventListener('change', toggleGraphiQLEditor);


    window['libAdmin'].store.get('projectContext').onProjectChanged(function () {
        toggleGraphiQLEditor();
    });

    renderGraphiQLUI();
});

function toggleGraphiQLEditor() {
    if (wsClient != null) {
        // TODO try to close connection
    }

    ReactDOM.unmountComponentAtNode(document.getElementById(`graphiql-container`));

    renderGraphiQLUI();
}

let wsClient = null;

function renderGraphiQLUI() {
    const container = document.getElementById(`graphiql-container`);

    const clientEndpoint = `ws://${window.location.host}${getHandlerUrl()}`;

    wsClient = graphqlWs.createClient(
        {
            url: clientEndpoint,
            lazy: true
        });

    const fetcher = GraphiQL.createFetcher({url: getHandlerUrl(), wsClient: wsClient});

    ReactDOM.render(
        React.createElement(GraphiQL, {
            fetcher: fetcher,
            defaultVariableEditorOpen: false
        }),
        container
    );
}

function waitFor(variable, callback) {
    let interval = setInterval(function () {
        if (window[variable]) {
            clearInterval(interval);
            callback();
        }
    }, 100);
}
