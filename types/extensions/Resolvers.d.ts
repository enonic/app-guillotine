import type {
	PartialRecord,
	ValueOf,
} from '../Utils'
import type {LocalContext} from '../graphQL/LocalContext'


export declare interface DataFetchingEnvironment<
	ARGS extends Record<string, any> = Record<string, any>,
	LOCAL_CONTEXT extends LocalContextRecord = LocalContextRecord,
	SOURCE extends unknown = unknown,
> {
	args: ARGS
	localContext: LocalContext<LOCAL_CONTEXT>
	source: SOURCE
}

export declare interface Resolver<
	ARGS extends Record<string, any> = Record<string, any>,
	LOCAL_CONTEXT extends LocalContextRecord = LocalContextRecord,
	SOURCE extends unknown = unknown,
	RETURN_TYPE = any
> {
	(env: DataFetchingEnvironment<ARGS,LOCAL_CONTEXT,SOURCE>): RETURN_TYPE
}

export declare type Resolvers = PartialRecord<
	keyof GraphQLObjectTypeFieldsMap,
	Record<
		keyof ValueOf<GraphQLObjectTypeFieldsMap>,
		Resolver
	>
>
