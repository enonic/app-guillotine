import type {
	PartialRecord,
	ValueOf,
} from '../Utils'
import type {LocalContext} from '../graphQL/LocalContext'


export declare interface DataFetchingEnvironment<
	Args extends Record<string, any> = Record<string, any>,
	Source extends unknown = unknown,
> {
	args: Args
	localContext: LocalContext
	source: Source
}

export declare interface Resolver<
	Args extends Record<string, any> = Record<string, any>,
	Source extends unknown = unknown,
	Return = any
> {
	(env: DataFetchingEnvironment<Args,Source>): Return
}

export declare type Resolvers = PartialRecord<
	keyof GraphQLObjectTypeFieldsMap,
	Record<
		keyof ValueOf<GraphQLObjectTypeFieldsMap>,
		Resolver
	>
>
