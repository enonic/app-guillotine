import type {
	GraphQLEnumType,
	GraphQLEnumTypeReference,
	GraphQLInputType,
	GraphQLInputTypeName,
	GraphQLInputTypeReference,
	GraphQLScalars
} from '../graphQL/'


export declare interface InputType {
	description?: string
	fields: Record<
		string, // Could make a global InputTypeFieldsMap
		| GraphQLEnumTypeReference<GraphQLEnumType>
		| GraphQLInputTypeReference<GraphQLInputType>
		| GraphQLScalars
	>
}

export declare type InputTypes = Record<GraphQLInputTypeName,InputType>
