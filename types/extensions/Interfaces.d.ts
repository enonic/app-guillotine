import type {
	GraphQLInterfaceTypeName
} from '../graphQL/InterfaceTypes'
import type {FieldsWithOptionalArgs} from './Field'


export declare type Interfaces = Record<
	GraphQLInterfaceTypeName,
	{
		description: string
		fields: FieldsWithOptionalArgs
	}
>
