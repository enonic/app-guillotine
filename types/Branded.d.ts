import type {ParsedJSON} from './Utils'


declare const __base: unique symbol
declare const __name: unique symbol
declare const __nonNull: unique symbol
declare const __reference: unique symbol
declare const __parsedJson: unique symbol


type Brand<
	Base,
	Name extends string,
	PJ extends ParsedJSON|undefined = undefined
> = {
	[__base]: Base
	[__name]: Name
	[__parsedJson]: PJ
}

type GraphQLBrand<
	Base,
	Name extends string,
	PJ extends ParsedJSON|undefined = undefined,
	NonNull extends boolean = false,
	Reference extends boolean = false,
> = {
	[__base]: Base
	[__name]: Name
	[__parsedJson]: PJ
	[__nonNull]: NonNull
	[__reference]: Reference
}

export type JustBrand = Brand<any, string, any>
export type AnyGraphQLBrand = GraphQLBrand<any, string, any, boolean, boolean>
// export type AnyJson = Brand<any, string, ParsedJSON>

export type Branded<
	Base,
	Name extends string,
	PJ extends ParsedJSON|undefined = undefined
> = Base & Brand<Base, Name, PJ>

export type GraphQLBranded<
	Base,
	Name extends string,
	PJ extends ParsedJSON|undefined = undefined,
	NonNull extends boolean = false,
	Reference extends boolean = false
> = Base & GraphQLBrand<Base, Name, PJ, NonNull, Reference>

export type GetBase<T> = T extends Brand<infer Base, any, any> ? Base : never
export type GetName<T> = T extends Brand<any, infer Name, any> ? Name : never
export type GetParsedJson<T> = T extends Brand<any, any, infer PJ> ? PJ : never

// export type GetBase<T> = T extends GraphQLBrand<infer Base, any, any, any> ? Base : never
// export type GetName<T> = T extends GraphQLBrand<any, infer Name, any, any> ? Name : never

export type GetNonNull<T> = T extends GraphQLBrand<any, any, any, infer NonNull, any> ? NonNull : never
export type GetReference<T> = T extends GraphQLBrand<any, any, any, any, infer Reference> ? Reference : never

export declare type NonNull<T> = GraphQLBranded<GetBase<T>, GetName<T>, GetParsedJson<T>, true, GetReference<T>>
export declare type Reference<T> = GraphQLBranded<GetBase<T>, GetName<T>, GetParsedJson<T>, GetNonNull<T>, true>

// export type BrandBuilder<T extends Branded<Base, any>, Base = BrandBase<T>> = {
// 	check: (value: Base) => value is T
// 	assert: (value: Base) => asserts value is T
// 	from: (value: Base) => T
// }

// export type BrandBuilderOptions<Base> = {
// 	validate?: (value: Base) => boolean | string
// }
