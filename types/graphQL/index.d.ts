/// <reference path="../Global.Modifying.d.ts"/>


import type {
	AnyGraphQLBrand,
	GetSuperType,
	NonNull,
	Reference,
} from '../brand'
import type {
	ArrayElement,
	PartialRecord,
	ValueOf,
} from '../Utils'
import type {CreateDataFetcherResult} from './CreateDataFetcherResult'
import type {
	GraphQLObjectType,
	GraphQLObjectTypeName
} from './ObjectTypes'
import type {
	GraphQLCustomScalars,
	GraphQLExtendedScalars,
	GraphQLScalars,
} from './ScalarTypes'


export type {
	CreateDataFetcherResult,
	CreateDataFetcherResultParams,
} from './CreateDataFetcherResult'

export type {
	GraphQLArgs,
	GraphQLInputType,
	GraphQLInputTypeName,
} from './InputTypes'

export type {
	GraphQLObjectType,
	GraphQLObjectTypeName,
} from './ObjectTypes'

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

export declare type GraphQLReference<
	GraphQLObjectType extends ValueOf<GraphQLObjectTypesMap>
> = Reference<GraphQLObjectType>


export declare interface GraphQL
	extends GraphQLScalars, GraphQLExtendedScalars, GraphQLCustomScalars
{
	createDataFetcherResult: CreateDataFetcherResult
	nonNull: (type: GraphQLObjectType) => GraphQLNonNull<GraphQLObjectType>
	list: (type: GraphQLObjectType) => (typeof type)[]
	reference: (typeKey: GraphQLObjectTypeName) => GraphQLReference<GraphQLObjectTypesMap[typeof typeKey]> // GraphQLReference<typeof typeKey>
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
	? GetSuperType<Type>
	: Type extends AnyGraphQLBrand[]
		? GetSuperType<ArrayElement<Type>>[]
		: Type extends Record<string,any>
			? {
				[fieldKey in keyof Type]: Type[fieldKey] extends AnyGraphQLBrand
				? GetSuperType<Type[fieldKey]>
				: Type[fieldKey] extends AnyGraphQLBrand[]
					? GetSuperType<ArrayElement<Type[fieldKey]>>[]
						: 'Record with unhandeled key'
			}
			: 'Unhandled type'
