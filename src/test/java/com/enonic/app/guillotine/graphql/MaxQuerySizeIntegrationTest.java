package com.enonic.app.guillotine.graphql;

import org.junit.jupiter.api.Test;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.parser.Parser;
import graphql.parser.ParserEnvironment;
import graphql.parser.ParserOptions;
import graphql.parser.exceptions.ParseCancelledException;
import graphql.schema.GraphQLSchema;

import static org.junit.Assert.assertThrows;

public class MaxQuerySizeIntegrationTest
	extends BaseGraphQLIntegrationTest
{

	@Test
	void shouldFailWhenMaxTokensExceeded()
	{
		int maxTokens = 15;
		ParserOptions parserOptions = ParserOptions.newParserOptions().maxTokens( maxTokens ).build();

		String query = ResourceHelper.readGraphQLQuery( "graphql/maxQuerySize.graphql" );

		GraphQLSchema schema = getBean().createSchema();
		GraphQL graphQL = GraphQL.newGraphQL( schema ).build();

		Parser parser = new Parser();

		assertThrows( ParseCancelledException.class, () -> {
			parser.parseDocument( ParserEnvironment.newParserEnvironment().document( query ).parserOptions( parserOptions ).build() );

			ExecutionInput executionInput = ExecutionInput.newExecutionInput().query( query ).build();
			graphQL.execute( executionInput );
		} );
	}
}
