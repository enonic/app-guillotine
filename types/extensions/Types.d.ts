import type {GraphQLObjectType} from '../graphQL/ObjectTypes'


export declare interface Type {
	description: string
	fields: Record<string, {
		type: GraphQLObjectType | GraphQLObjectType[]
	}>
}

export declare type Types = Record<string, Type>
