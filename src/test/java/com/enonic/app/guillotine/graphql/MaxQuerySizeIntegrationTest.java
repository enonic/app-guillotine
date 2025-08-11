package com.enonic.app.guillotine.graphql;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.parser.Parser;
import graphql.parser.ParserEnvironment;
import graphql.parser.ParserOptions;
import graphql.parser.exceptions.ParseCancelledException;
import graphql.schema.GraphQLSchema;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

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


	@Test
	void testMaxTokensForBigQuery()
	{
		String query = generateBigQuery( 5000 );

		when( guillotineConfigService.getMaxQueryTokens() ).thenReturn( 15001 );

		GraphQLSchema graphQLSchema = getBean().createSchema();

		Map<String, Object> result = executeQuery( graphQLSchema, query );

		assertTrue( result.containsKey( "errors" ) );

		List<Map<String, String>> errors = (List<Map<String, String>>) result.get( "errors" );

		assertEquals( 1, errors.size() );

		assertEquals( "More than 15,001 'grammar' tokens have been presented. To prevent Denial Of Service attacks, parsing has been cancelled.", errors.get( 0 ).get( "message" ) );


		when( guillotineConfigService.getMaxQueryTokens() ).thenReturn( 15100 );
		result = executeQuery( graphQLSchema, query );

		assertFalse( result.containsKey( "errors" ) );
	}

	private String generateBigQuery( int count )
	{
		StringBuilder builder = new StringBuilder();
		builder.append( "query {\n" );
		builder.append( "  guillotine {\n" );
		builder.append( "    get(key: \"/\") {\n" );
		builder.append( "      _id\n" );
		builder.append( "      displayName\n" );

		for ( int i = 1; i <= count; i++ )
		{
			builder.append( "      alias" ).append( i ).append( ": displayName\n" );
		}

		builder.append( "    }\n" );
		builder.append( "  }\n" );
		builder.append( "}\n" );

		return builder.toString();
	}
}
