/// <reference path="../Global.Modifying.d.ts"/>


import type {ValueOf} from '../Utils'
import type {GraphQLScalars} from './ScalarTypes'


export declare type GraphQLInputType = ValueOf<GraphQLInputTypesMap>
export declare type GraphQLInputTypeName = keyof GraphQLInputTypesMap

export declare type GraphQLArgs = Record<string,
	| GraphQLScalars
	| GraphQLInputType
>
