import type {GraphQLArgs} from '../graphQL/InputTypes'
import type {GraphQLObjectType} from '../graphQL/ObjectTypes'
import type {GraphQLObjectTypeReference} from '../graphQL/ReferenceTypes'
import type {GraphQLScalars} from '../graphQL/ScalarTypes'


export declare interface Field {
	type:
		| GraphQLScalars
		| GraphQLObjectType
		| GraphQLObjectTypeReference<GraphQLObjectType>
		// | GraphQLEnumType doesn't make sense here?
		// | GraphQLInputType doesn't make sense here.
}

export declare type Fields = Record<string, Field>

export declare interface FieldWithOptionalArgs extends Field {
	args?: GraphQLArgs
}

export declare type FieldsWithOptionalArgs = Record<string, FieldWithOptionalArgs>
