import type {
	GraphQLEnumType,
	GraphQLEnumTypeReference,
	GraphQLInputType,
	GraphQLInputTypeName,
	GraphQLInputTypeReference,
	GraphQLScalars
} from '../graphQL/'


export declare type InputTypeField =
	| GraphQLEnumTypeReference<GraphQLEnumType>
	| GraphQLInputTypeReference<GraphQLInputType>
	| GraphQLScalars

export declare type InputTypeFields = Record<
	string, // Could make a global InputTypeFieldsMap
	InputTypeField
>

export declare interface InputType {
	description?: string
	fields: InputTypeFields
}

export declare type InputTypes = Record<GraphQLInputTypeName,InputType>
