// Make sure ScriptValue type exists in global scope:
/// <reference types="@enonic-types/global"/>

//// <reference path="./Global.Modifying.d.ts"/>

import type {
	AnyGraphQLBrand,
	GetBase,
} from './Branded'
import type {
	GraphQLArgs,
	GraphQLCustomScalars,
	GraphQLExtendedScalars,
	// GraphQLInputType,
	// GraphQLInputTypeName,
	GraphQLNonNull,
	GraphQLReference,
	GraphQLScalars,
	GraphQLString,
	GraphQLObjectType,
	GraphQLObjectTypeName,
} from './GraphQL'
import type {
	ArrayElement,
	PartialRecord,
	ValueOf
} from './Utils'


//──────────────────────────────────────────────────────────────────────────────
// GraphQL:
//──────────────────────────────────────────────────────────────────────────────
export declare type LocalContext = Record<string,string|number|boolean|null>

export declare type LocalContextGeneric<
	T extends Record<string,string|number|boolean|null> = Record<string,string|number|boolean|null>
> = {
	branch: string
	project: string
	siteKey?: string
} & T

export declare interface CreateDataFetcherResultParams<
	In extends LocalContext = LocalContext,
	Out extends LocalContext = LocalContext
> {
	data: ScriptValue // NOTE: ScriptValue type is expected to exist in global scope
	localContext?: LocalContextGeneric<Out>
	parentLocalContext?: LocalContextGeneric<In>
}

export declare interface GraphQL
	extends GraphQLScalars, GraphQLExtendedScalars, GraphQLCustomScalars
{
	createDataFetcherResult: <
		In extends LocalContext = LocalContext,
		Out extends LocalContext = LocalContext
	>(params: CreateDataFetcherResultParams<In,Out>) => LocalContextGeneric<In&Out>
	nonNull: (type: GraphQLObjectType) => GraphQLNonNull<GraphQLObjectType>
	list: (type: GraphQLObjectType) => (typeof type)[]
	reference: (typeKey: GraphQLObjectTypeName) => GraphQLReference<GraphQLObjectTypesMap[typeof typeKey]> // GraphQLReference<typeof typeKey>
}


//──────────────────────────────────────────────────────────────────────────────
// Extenstions:
//──────────────────────────────────────────────────────────────────────────────
export declare interface CreationCallback {
	(params: {
		addFields: (newFields: Record<string, {
			args?: GraphQLArgs
			type: GraphQLObjectType
		}>) => void
		modifyFields: (existingFields: Record<string, {
			args?: GraphQLArgs
			type: GraphQLObjectType
		}>) => void
		removeFields: (existingFields: string[]) => void
		setDescription: (newDescription: string) => void
		// TODO
		// setInterfaces: (reWrittenInterfaces: (GraphQLInterfaceType|GraphQLReferenceType)[]) => void
	}): void
}

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

export declare interface Type {
	description: string
	fields: Record<string, {
		type: GraphQLObjectType | GraphQLObjectType[]
	}>
}

export declare interface Extensions {
	creationCallbacks?: Record<string, CreationCallback>
	// enums?: Record<string, ExtensionEnum>
	// inputTypes?: Record<string, any>
	// interfaces?: Record<string, any>
	//resolvers?: PartialRecord<GraphQLObjectTypeName, PartialRecord<GraphQLFieldName, Resolver>>
	resolvers?: PartialRecord<
		keyof GraphQLObjectTypeFieldsMap,
		Record<
			keyof ValueOf<GraphQLObjectTypeFieldsMap>,
			Resolver
		>
	>
	// typeResolvers?: Record<string, any>
	types?: Record<string, Type>
	// unions?: Record<string, any>
}

//──────────────────────────────────────────────────────────────────────────────
// Converters:
//──────────────────────────────────────────────────────────────────────────────

export type GraphQLTypeToGuillotineFields<
	Type // extends ValueOf<GraphQLObjectTypesMap>
> = {
	[fieldKey in keyof Type]: {
		type: Type[fieldKey]
	}
}

// type BasicTypes = boolean|number|string


export type GraphQLTypeToResolverResult<
	Type // extends ValueOf<GraphQLObjectTypesMap>
> =
	Type extends AnyGraphQLBrand
	? GetBase<Type>
	: Type extends AnyGraphQLBrand[]
		? GetBase<ArrayElement<Type>>[]
		: Type extends Record<string,any>
			? {
				[fieldKey in keyof Type]: Type[fieldKey] extends AnyGraphQLBrand
				? GetBase<Type[fieldKey]>
				: Type[fieldKey] extends AnyGraphQLBrand[]
					? GetBase<ArrayElement<Type[fieldKey]>>[]
						: 'Record with unhandeled key'
			}
			: 'Unhandled type'
