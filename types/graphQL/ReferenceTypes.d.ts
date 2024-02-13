/// <reference path="../Global.Modifying.d.ts"/>


import type {
	Reference,
} from '../brand'
import type {
	ValueOf,
} from '../Utils'
import type {
	GraphQLEnumType,
} from './EnumTypes'
import type {
	GraphQLInputType,
} from './InputTypes'
import type {
	GraphQLInterfaceType,
} from './InterfaceTypes'
import type {
	GraphQLObjectType,
} from './ObjectTypes'
import type {
	GraphQLUnionType,
} from './UnionTypes'


// export declare type GraphQLReference<
// 	GraphQLEnumOrInputOrInterfaceOrObjectOrUnionType extends ValueOf<
// 		GraphQLEnumTypesMap
// 		& GraphQLInputTypesMap
// 		& GraphQLInterfaceTypesMap
// 		& GraphQLObjectTypesMap
// 		& GraphQLUnionTypesMap
// 	>
// > = Reference<GraphQLEnumOrInputOrInterfaceOrObjectOrUnionType>

export declare type GraphQLEnumTypeReference<
	GraphQLEnumType extends ValueOf<GraphQLEnumTypesMap>
> = Reference<GraphQLEnumType>

export declare type GraphQLInputTypeReference<
	GraphQLInputType extends ValueOf<GraphQLInputTypesMap>
> = Reference<GraphQLInputType>

export declare type GraphQLInterfaceTypeReference<
	GraphQLInterfaceType extends ValueOf<GraphQLInterfaceTypesMap>
> = Reference<GraphQLInterfaceType>

export declare type GraphQLObjectTypeReference<
	GraphQLObjectType extends ValueOf<GraphQLObjectTypesMap>
> = Reference<GraphQLObjectType>

export declare type GraphQLUnionTypeReference<
	GraphQLUnionType extends ValueOf<GraphQLUnionTypesMap>
> = Reference<GraphQLUnionType>
