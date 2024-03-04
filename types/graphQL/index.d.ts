/// <reference path="../Global.Modifying.d.ts"/>


import type {
	AnyGraphQLBrand,
	GetReturnType,
	NonNull,
	Reference,
} from '../brand'
import type {
	ArrayElement,
	PartialRecord,
	ValueOf,
} from '../Utils'
import type {
	CreateDataFetcherResult,
	CreateDataFetcherResultParams,
	DataFetcherResult,
} from './CreateDataFetcherResult'
import type {
	GraphQLEnumType,
	GraphQLEnumTypeName,
} from './EnumTypes'
import type {
	GraphQLInputType,
	GraphQLInputTypeName,
} from './InputTypes'
import type {
	GraphQLInterfaceType,
	GraphQLInterfaceTypeName,
} from './InterfaceTypes'
import type {
	GraphQLObjectType,
	GraphQLObjectTypeName
} from './ObjectTypes'
import type {
	GraphQLEnumTypeReference,
	GraphQLInputTypeReference,
	GraphQLInterfaceTypeReference,
	GraphQLObjectTypeReference,
	GraphQLUnionTypeReference,
} from './ReferenceTypes'
import type {
	GraphQLCustomScalars,
	GraphQLExtendedScalars,
	GraphQLScalars,
} from './ScalarTypes'
import type {
	GraphQLUnionType,
	GraphQLUnionTypeName
} from './UnionTypes'


export type {
	CreateDataFetcherResult,
	CreateDataFetcherResultParams,
} from './CreateDataFetcherResult'

export type {
	GraphQLEnumType,
	GraphQLEnumTypeName,
} from './EnumTypes'

export type {
	GraphQLArgs,
	GraphQLInputType,
	GraphQLInputTypeName,
} from './InputTypes'

export type {
	GraphQLInterfaceType,
	GraphQLInterfaceTypeName,
} from './InterfaceTypes'

export type {
	LocalContext,
	LocalContextRecord,
} from './LocalContext'

export type {
	GraphQLObjectType,
	GraphQLObjectTypeName,
} from './ObjectTypes'

export type {
	GraphQLEnumTypeReference,
	GraphQLInputTypeReference,
	GraphQLInterfaceTypeReference,
	GraphQLObjectTypeReference,
	GraphQLUnionTypeReference,
} from './ReferenceTypes'

export type {
	GraphQLBaseScalars,
	GraphQLBoolean,
	GraphQLCustomScalars,
	GraphQLDate,
	GraphQLDateTime,
	GraphQLExtendedScalars,
	GraphQLFloat,
	GraphQLID,
	GraphQLInt,
	GraphQLJson,
	GraphQLLocalDateTime,
	GraphQLLocalTime,
	GraphQLScalars,
	GraphQLString
} from './ScalarTypes'


export declare type GraphQLNonNull<
	GraphQLObjectType extends ValueOf<GraphQLObjectTypesMap>
> = NonNull<GraphQLObjectType>

export declare type GraphQLType =
	| GraphQLEnumType
	| GraphQLInputType
	| GraphQLObjectType
	| GraphQLScalars

export declare interface GraphQL
	extends GraphQLScalars//, GraphQLExtendedScalars, GraphQLCustomScalars
{
	createDataFetcherResult: CreateDataFetcherResult
	nonNull: (type: GraphQLType) => GraphQLNonNull<typeof type>
	list: (type: GraphQLType) => (typeof type)[]
	reference: (typeName: string) =>
		typeof typeName extends GraphQLEnumTypeName
			? GraphQLEnumTypeReference<GraphQLEnumTypesMap[typeof typeName]>
			: typeof typeName extends GraphQLInputTypeName
				? GraphQLInputTypeReference<GraphQLInputTypesMap[typeof typeName]>
				: typeof typeName extends GraphQLInterfaceTypeName
					? GraphQLInterfaceTypeReference<GraphQLInterfaceTypesMap[typeof typeName]>
					: typeof typeName extends GraphQLObjectTypeName
						? GraphQLObjectTypeReference<GraphQLObjectTypesMap[typeof typeName]>
						: typeof typeName extends GraphQLUnionTypeName
							? GraphQLUnionTypeReference<GraphQLUnionTypesMap[typeof typeName]>
							: 'reference(typeName) not registered in global GraphQLEnumTypesMap, GraphQLInputTypesMap, GraphQLInterfaceTypesMap, GraphQLObjectTypesMap or GraphQLUnionTypesMap'
	// GraphQLReference<(GraphQLInputTypesMap & GraphQLObjectTypesMap)[typeof typeKey]> // GraphQLReference<typeof typeKey>
}

//──────────────────────────────────────────────────────────────────────────────
// Converters:
//──────────────────────────────────────────────────────────────────────────────

export declare type GraphQLTypeToGuillotineFields<
	Type // extends ValueOf<GraphQLObjectTypesMap>
> = {
	[fieldKey in keyof Type]: {
		type: Type[fieldKey]
	}
}

// type BasicTypes = boolean|number|string

// Note: This is sketchy, but perhaps useful?
export declare type GraphQLTypeToResolverResult<
	Type // extends ValueOf<GraphQLObjectTypesMap>
> =
	Type extends AnyGraphQLBrand
	? GetReturnType<Type>
	: Type extends AnyGraphQLBrand[]
		? GetReturnType<ArrayElement<Type>>[]
		: Type extends Record<string,any>
			? {
				[fieldKey in keyof Type]: Type[fieldKey] extends AnyGraphQLBrand
				? GetReturnType<Type[fieldKey]>
				: Type[fieldKey] extends AnyGraphQLBrand[]
					? GetReturnType<ArrayElement<Type[fieldKey]>>[]
						: 'Record with unhandeled key'
			}
			: 'Unhandled type'
