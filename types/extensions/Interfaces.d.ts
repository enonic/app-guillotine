import type {
	GraphQLInterfaceTypeName
} from '../graphQL/InterfaceTypes'
import type {Fields} from './Field'


export declare type Interfaces = Record<
	GraphQLInterfaceTypeName,
	{
		description: string
		fields: Fields
	}
>
