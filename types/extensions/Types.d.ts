import type {
	GraphQLInterfaceType,
	GraphQLInterfaceTypeReference,
	GraphQLObjectType,
} from '../graphQL'
import type {Fields} from './Field'


export declare interface Type {
	description: string
	fields: Fields
	interfaces: GraphQLInterfaceTypeReference<GraphQLInterfaceType>[]
}

export declare type Types = Record<string, Type>
