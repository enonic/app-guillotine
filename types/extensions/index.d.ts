import type {CreationCallbacks} from './CreationCallbacks'
import type {Enums} from './Enums'
import type {InputTypes} from './InputTypes'
import type {Interfaces} from './Interfaces'
import type {Resolvers} from './Resolvers'
import type {Types} from './Types'
import type {TypeResolvers} from './TypeResolvers'
import type {Unions} from './Unions'


export type {
	CreationCallback,
	CreationCallbacks,
} from './CreationCallbacks'

export type {
	Enum,
	Enums,
} from './Enums'

export type {
	Field,
	Fields,
}

export type {
	InputType,
	InputTypeField,
	InputTypeFields,
	InputTypes,
} from './InputTypes'

export type {DataFetchingEnvironment} from './DataFetchingEnvironment'
export type {
	AnyResolver,
	Resolver
} from './Resolver'
export type {Resolvers} from './Resolvers'

export type {
	Type,
	Types
} from './Types'

export type {
	TypeResolver,
	TypeResolvers,
} from './TypeResolvers'

export type {
	Union,
	Unions,
} from './Unions'

export declare interface Extensions {
	creationCallbacks?: CreationCallbacks
	enums?: Enums
	inputTypes?: InputTypes
	interfaces?: Interfaces
	resolvers?: Resolvers
	typeResolvers?: TypeResolvers
	types?: Types
	unions?: Unions
}
