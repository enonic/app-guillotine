import type {
	GraphQLInputType,
	GraphQLInputTypeName
} from '../graphQL/InputTypes'
import type {GraphQLScalars} from '../graphQL/ScalarTypes'
import type {GraphQLReference} from '../graphQL/'


export declare type InputTypes = Record<
	GraphQLInputTypeName,
	{
		description?: string
		fields: Record<
			string, // Could make a global InputTypeFieldsMap
			GraphQLScalars | GraphQLReference<GraphQLInputType>
		>
	}
>
