/// <reference path="./Global.Modifying.d.ts"/>

import type {
	PartialRecord,
	ValueOf,
} from '../Utils'
import type {Resolver} from './Resolver'


export declare type Resolvers = PartialRecord<
	keyof GraphQLObjectTypeFieldsMap,
	Record<
		keyof ValueOf<GraphQLObjectTypeFieldsMap>,
		Resolver
	>
>
