import type {
	GraphQLObjectType,
	GraphQLObjectTypeReference,
} from "../graphQL"


export declare interface Union {
	description: string
	types: GraphQLObjectTypeReference<GraphQLObjectType>[]
}

export declare type Unions = Record<string, Union>
