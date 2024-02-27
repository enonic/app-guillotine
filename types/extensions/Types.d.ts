import type {
	GraphQLInterfaceType,
	GraphQLInterfaceTypeReference,
	GraphQLObjectType,
} from '../graphQL'
import type {Field} from './Field'


export declare interface Type {
	description: string
	fields: Record<string, Field>
	interfaces: GraphQLInterfaceTypeReference<GraphQLInterfaceType>[]
}

export declare type Types = Record<string, Type>
