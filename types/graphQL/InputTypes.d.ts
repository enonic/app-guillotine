/// <reference path="../Global.Modifying.d.ts"/>


import type {ValueOf} from '../Utils'
import type {GraphQLEnumType} from './EnumTypes'
import type {
	GraphQLEnumTypeReference,
	GraphQLInputTypeReference,
} from './ReferenceTypes'
import type {GraphQLScalars} from './ScalarTypes'


export declare type GraphQLInputType = ValueOf<GraphQLInputTypesMap>
export declare type GraphQLInputTypeName = keyof GraphQLInputTypesMap

export declare type GraphQLArgs = Record<string,
	| GraphQLEnumType
	| GraphQLEnumTypeReference<GraphQLEnumType>
	| GraphQLInputType
	| GraphQLInputTypeReference<GraphQLInputType>
	| GraphQLScalars
	// GraphQLObjectType doesn't make sense here.
>
