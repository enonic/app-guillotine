// Make sure ScriptValue type exists in global scope:
/// <reference types="@enonic-types/global"/>


declare const __name: unique symbol

declare type BrandGraphQLScalarType<
	GQL_SCALAR_TYPE_NAME extends string,
	SCALAR_TYPE extends boolean|string|number
> = SCALAR_TYPE & {
	[__name]: GQL_SCALAR_TYPE_NAME
}

export declare type GraphQLBoolean = BrandGraphQLScalarType<'GraphQLBoolean', boolean>
export declare type GraphQLDate = BrandGraphQLScalarType<'Date', string>
export declare type GraphQLDateTime = BrandGraphQLScalarType<'DateTime', string>
export declare type GraphQLFloat = BrandGraphQLScalarType<'GraphQLFloat', number>
export declare type GraphQLID = BrandGraphQLScalarType<'GraphQLID', string>
export declare type GraphQLInt = BrandGraphQLScalarType<'GraphQLInt', number>
export declare type GraphQLJson = BrandGraphQLScalarType<'Json', string>
export declare type GraphQLLocalDateTime = BrandGraphQLScalarType<'LocalDateTime', string>
export declare type GraphQLLocalTime = BrandGraphQLScalarType<'LocalTime', string>
export declare type GraphQLString = BrandGraphQLScalarType<'GraphQLString', string>

export declare type GraphQLType = any

export declare interface GraphQL {
	Date: GraphQLDate
	DateTime: GraphQLDateTime
	GraphQLBoolean: GraphQLBoolean
	GraphQLFloat: GraphQLFloat
	GraphQLID: GraphQLID
	GraphQLInt: GraphQLInt
	GraphQLString: GraphQLString
	Json: GraphQLJson
	LocalDateTime: GraphQLLocalDateTime
	LocalTime: GraphQLLocalTime
	createDataFetcherResult: <
	In extends LocalContextRecord = LocalContext,
	Out extends LocalContextRecord = LocalContext
> () => {
		data: ScriptValue // NOTE: ScriptValue type is expected to exist in global scope
		localContext?: LocalContext<Out>
		parentLocalContext?: LocalContext<In>
	}
	nonNull: (type: GraphQLType) => GraphQLType
	list: (type: GraphQLType) => GraphQLType[]
	reference: (typeName: string) => GraphQLType
}

export declare type GraphQLArgs = Record<string, GraphQLType | GraphQLType[]>

export declare interface CreationCallback {
	(params: {
		addFields: (newFields: Record<string, {
			args?: GraphQLArgs
			type: GraphQLType | GraphQLType[]
		}>) => void
		modifyFields: (existingFields: Record<string, {
			args?: GraphQLArgs
			type: GraphQLType | GraphQLType[]
		}>) => void
		removeFields: (existingFields: string[]) => void
		setDescription: (newDescription: string) => void
		// TODO
		// setInterfaces: (reWrittenInterfaces: (GraphQLInterfaceType|GraphQLReferenceType)[]) => void
	}): void
}

export declare interface Enum {
	description: string
	values: Record<string, string>
}

export declare type LocalContextRecord = Record<string,string|number|boolean|null>

export declare type LocalContext<
	T extends LocalContextRecord = LocalContextRecord
> = {
	branch: string
	project: string
	siteKey?: string
} & T

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

export declare interface Extensions {
	creationCallbacks?: Record<string, CreationCallback>
	enums?: Record<
		string,
		Enum
	>
	inputTypes?: Record<
		string,
		{
			description?: string
			fields: Record<
				string,
				GraphQLType | GraphQLType[]
			>
		}
	>
	interfaces?: Record<string, {
		description: string
		fields: Record<string, {
			args?: GraphQLArgs
			type: GraphQLType | GraphQLType[]
		}>
	}>
	resolvers?: Record<
		string,
		Record<
			string,
			Resolver
		>
	>
	typeResolvers?: Record<string, (param: any) => string>
	types?: Record<string, {
		description: string
		fields: Record<string, {
			type: GraphQLType | GraphQLType[]
		}>
	}>
	unions?: Record<string, {
		description: string
		types: GraphQLType[]
	}>
}
