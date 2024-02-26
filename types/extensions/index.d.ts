import type {CreationCallbacks} from './CreationCallbacks'
import type {Enums} from './Enums'
import type {InputTypes} from './InputTypes'
import type {Resolvers} from './Resolvers'
import type {Types} from './Types'


export type {
	CreationCallback,
	CreationCallbacks,
} from './CreationCallbacks'

export type {
	LocalContext,
	LocalContextRecord,
} from '../graphQL/LocalContext'

export type {
	DataFetchingEnvironment,
	Resolver,
	Resolvers,
} from './Resolvers'

export type {
	Type,
	Types
} from './Types'



export declare interface Extensions {
	creationCallbacks?: CreationCallbacks
	enums?: Enums
	inputTypes?: InputTypes
	// interfaces?: Record<string, any>
	resolvers?: Resolvers
	// typeResolvers?: Record<string, any>
	types?: Types
	// unions?: Record<string, any>
}
