function graphQLFetcher(graphQLParams) {
    const projectVal = window.libAdmin.store.get('projectContext').currentProject.name;
    const branchVal = document.getElementById('branch').value;

    return fetch(
        `/admin/site/preview/${projectVal}/${branchVal}`,
        {
            method: 'post',
            headers: {
                Accept: 'application/json',
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(graphQLParams),
            credentials: 'include',
        },
    ).then(function (response) {
        return response.json().catch(function () {
            return response.text();
        });
    });
}

waitFor('GraphiQL', function () {
    ReactDOM.render(
        React.createElement(GraphiQL, {
            fetcher: graphQLFetcher,
            defaultVariableEditorOpen: true,
        }),
        document.getElementById('graphiql-container'),
    );
});

function waitFor(variable, callback) {
    let interval = setInterval(function () {
        if (window[variable]) {
            clearInterval(interval);
            callback();
        }
    }, 100);
}
