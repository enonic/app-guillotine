function graphQLFetcher(graphQLParams) {
    const projectVal = document.getElementById('project').value;
    const branchVal = document.getElementById('branch').value;

    return fetch(
        `/webapp/com.enonic.app.guillotine/${projectVal}/${branchVal}`,
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

ReactDOM.render(
    React.createElement(GraphiQL, {
        fetcher: graphQLFetcher,
        defaultVariableEditorOpen: true,
    }),
    document.getElementById('graphiql-container'),
);
