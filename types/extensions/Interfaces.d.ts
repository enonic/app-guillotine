import type {
	GraphQLInterfaceTypeName
} from '../graphQL/InterfaceTypes'
import type {Field} from './Field'


export declare type Interfaces = Record<
	GraphQLInterfaceTypeName,
	{
		description: string
		fields: Record<string, Field>
	}
>
