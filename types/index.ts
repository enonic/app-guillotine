// Make sure ScriptValue type exists in global scope:
/// <reference types="@enonic-types/global"/>

import type {CreateDataFetcherResult} from './graphQL/CreateDataFetcherResult'
import type {AnyResolver} from './extensions/Resolver'

declare const __name: unique symbol

declare type BrandGraphQLScalarType<
	GQL_SCALAR_TYPE_NAME extends string,
	SCALAR_TYPE extends boolean|string|number
> = SCALAR_TYPE & {
	[__name]: GQL_SCALAR_TYPE_NAME
}


export type {
	CreateDataFetcherResult,
	CreateDataFetcherResultParams,
	DataFetcherResult,
} from './graphQL/CreateDataFetcherResult'

export type {
	LocalContext,
	LocalContextRecord,
} from './graphQL/LocalContext'

export type {DataFetchingEnvironment} from './extensions/DataFetchingEnvironment'
export type {Resolver} from './extensions/Resolver'


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
	createDataFetcherResult: CreateDataFetcherResult
	nonNull: (type: GraphQLType) => GraphQLType
	list: (type: GraphQLType) => GraphQLType[]
	reference: (typeName: string) => GraphQLType
}

export declare type GraphQLArgs = Record<string, GraphQLType | GraphQLType[]>

export declare interface Field {
	args?: GraphQLArgs
	type: GraphQLType | GraphQLType[]
}

export declare type Fields = Record<string, Field>

export declare interface CreationCallback {
	(params: {
		addFields: (newFields: Fields) => void
		modifyFields: (existingFields: Fields) => void
		removeFields: (existingFields: string[]) => void
		setDescription: (newDescription: string) => void
		setInterfaces: (reWrittenInterfaces: GraphQLType[]) => void
	}): void
}

export declare type CreationCallbacks = Record<string, CreationCallback>

export declare interface Enum {
	description: string
	values: Record<string, string>
}

export declare type Enums = Record<string, Enum>

export declare interface Type {
	description: string
	fields: Fields
}

export declare type Types = Record<string, Type>

export declare interface InputTypeField {
	type: GraphQLType | GraphQLType[]
}

export declare type InputTypeFields = Record<string, InputTypeField>

export declare interface InputType {
	description?: string
	fields: InputTypeFields
}

export declare type InputTypes = Record<string, InputType>

export declare interface Interface {
	description: string
	fields: Fields
}

export declare type Interfaces = Record<string, Interface>

export declare interface Union {
	description: string
	types: GraphQLType[]
}

export declare type Unions = Record<string, Union>

export declare interface Extensions {
	creationCallbacks?: CreationCallbacks
	enums?: Enums
	inputTypes?: InputTypes
	interfaces?: Interfaces
	resolvers?: Record<
		string,
		Record<
			string,
			AnyResolver
		>
	>
	typeResolvers?: Record<string, (param: any) => string>
	types?: Types
	unions?: Unions
}


export type {
	BaseFolderContent,
	MediaImageContent,
	PortalSiteContent,
} from './xp'


export {
	EnumTypeName,
	FormItemType,
	InputTypeName,
	ObjectTypeName,
	Permission,
	PrincipalType,
	ScalarTypeName,
} from './guillotine'
