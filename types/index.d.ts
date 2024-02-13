/// <reference path="./Global.Modifying.d.ts"/>


import type {
	Extensions as GuillotineExtensions,
	GraphQL as GuillotineGraphQL,
} from './Guillotine'


export type {
	Branded,
	GraphQLBranded,
} from './Branded'

export type {
	GraphQLArgs,
	GraphQLBoolean,
	GraphQLDate,
	GraphQLDateTime,
	GraphQLFloat,
	GraphQLID,
	GraphQLInt,
	GraphQLJson,
	GraphQLLocalDateTime,
	GraphQLLocalTime,
	GraphQLNonNull,
	GraphQLReference,
	GraphQLString
} from './GraphQL'

export type {
	GraphQLTypeToGuillotineFields,
	GraphQLTypeToResolverResult,
	Resolver,
} from './Guillotine'

// export type {
// 	GraphQLObjectTypeName
// } from './Guillotine'

// export type {
// 	PartialRecord,
// } from './Utils'

export declare namespace Guillotine {
	export type Extensions = GuillotineExtensions
	export type GraphQL = GuillotineGraphQL
}
