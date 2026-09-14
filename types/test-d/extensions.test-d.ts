import {
	expectAssignable,
	expectError,
	expectType,
} from 'tsd';
import type {
	DataFetchingEnvironment,
	Extensions,
	GraphQL,
} from '../index';
import {
	EnumTypeName,
	InputTypeName,
	ObjectTypeName,
} from '../index';
import type {
	Component,
	Content,
	FormInput,
	FormItem,
	GraphQLDateTime,
	GraphQLJson,
	GraphQLString,
	HeadlessCms,
	PublishInfo,
	Query,
	QueryDSLContentConnection,
} from '../advanced';


declare const graphQL: GraphQL;
declare const env: DataFetchingEnvironment;

//──────────────────────────────────────────────────────────────────────────────
// createDataFetcherResult takes a single params object
//──────────────────────────────────────────────────────────────────────────────

expectError(graphQL.createDataFetcherResult());

graphQL.createDataFetcherResult({
	data: __.toScriptValue({id: '100'}),
	localContext: {
		parentId: '101',
	},
	parentLocalContext: env.localContext,
});

graphQL.createDataFetcherResult({
	data: __.toScriptValue({}),
});

// primitives may be passed without __.toScriptValue
graphQL.createDataFetcherResult({data: 'hello'});
graphQL.createDataFetcherResult({data: 42});
graphQL.createDataFetcherResult({data: true});

// plain objects must be wrapped with __.toScriptValue
expectError(graphQL.createDataFetcherResult({data: {id: '100'}}));

// localContext values must be primitives
expectError(graphQL.createDataFetcherResult({
	data: __.toScriptValue({}),
	localContext: {
		a: [1, 2, 3],
	},
}));

//──────────────────────────────────────────────────────────────────────────────
// Extensions.types supports interfaces and field args
//──────────────────────────────────────────────────────────────────────────────

expectAssignable<Extensions>({
	types: {
		CustomInterfaceImpl: {
			description: 'CustomInterface Implementation',
			interfaces: [graphQL.reference('CustomInterface')],
			fields: {
				query: {
					type: graphQL.list(graphQL.GraphQLString),
					args: {
						filter: graphQL.reference('CustomFilterInput'),
					},
				},
			},
		},
	},
});

//──────────────────────────────────────────────────────────────────────────────
// Typed resolvers are accepted by Extensions.resolvers
//──────────────────────────────────────────────────────────────────────────────

interface Source {
	_id: string;
}

expectAssignable<Extensions>({
	resolvers: {
		MyCustomType: {
			myCustomField: (env: DataFetchingEnvironment<{queryString: string}, Source>): string =>
				`${env.args.queryString}:${env.source._id}`,
			myDataFetcherResult: (env) => graphQL.createDataFetcherResult({
				data: __.toScriptValue({}),
				parentLocalContext: env.localContext,
			}),
		},
	},
});

//──────────────────────────────────────────────────────────────────────────────
// Built-in declarations follow the schema
//──────────────────────────────────────────────────────────────────────────────

declare const publishInfo: PublishInfo;
expectType<GraphQLDateTime>(publishInfo.from);
expectType<GraphQLDateTime>(publishInfo.to);
expectType<GraphQLDateTime>(publishInfo.first);
expectType<GraphQLDateTime>(publishInfo.time);

declare const content: Content;
expectType<GraphQLString>(content._project);
expectType<GraphQLString>(content._branch);
expectType<Component[]>(content.components);

declare const query: Query;
expectType<HeadlessCms>(query.guillotine);
expectType<Content>(query.guillotine.get);
expectType<QueryDSLContentConnection>(query.guillotine.queryDslConnection);
expectType<GraphQLJson>(query.guillotine.queryDslConnection.aggregationsAsJson);

declare const formInput: FormInput;
expectAssignable<FormItem>(formInput);
expectType<GraphQLString>(formInput.inputType);

//──────────────────────────────────────────────────────────────────────────────
// Removed schema names are gone from the type-name enums
//──────────────────────────────────────────────────────────────────────────────

// Type-level key lookup; never executed, tsd only type checks.
declare function hasKey<E extends object, K extends string>(e: E, key: K): K extends keyof E ? true : false;

expectType<false>(hasKey(EnumTypeName, 'ContentPathType'));
expectType<false>(hasKey(ObjectTypeName, 'DefaultValue'));
expectType<false>(hasKey(ObjectTypeName, 'ExtraData'));
expectType<false>(hasKey(ObjectTypeName, 'QueryContentConnection'));
expectType<false>(hasKey(InputTypeName, 'FilterInput'));
expectType<false>(hasKey(InputTypeName, 'BooleanFilterInput'));
expectType<false>(hasKey(InputTypeName, 'ExistsFilterInput'));
expectType<false>(hasKey(InputTypeName, 'NotExistsFilterInput'));
expectType<false>(hasKey(InputTypeName, 'HasValueFilterInput'));
expectType<false>(hasKey(InputTypeName, 'IdsFilterInput'));
expectType<false>(hasKey(InputTypeName, 'FormInput'));

expectType<true>(hasKey(EnumTypeName, 'ComponentType'));
expectType<true>(hasKey(ObjectTypeName, 'Mixin'));
expectType<true>(hasKey(ObjectTypeName, 'FormInput'));
expectType<true>(hasKey(ObjectTypeName, 'Query'));
expectType<true>(hasKey(ObjectTypeName, 'QueryDSLContentConnection'));
