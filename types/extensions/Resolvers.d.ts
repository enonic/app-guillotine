/// <reference path="./Global.Modifying.d.ts"/>

import type {
	PartialRecord,
	ValueOf,
} from '../Utils'
import type {AnyResolver} from './Resolver'


export declare type Resolvers = PartialRecord<
	keyof GraphQLObjectTypeFieldsMap,
	Record<
		keyof ValueOf<GraphQLObjectTypeFieldsMap>,
		AnyResolver
	>
>
