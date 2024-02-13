declare const __base: unique symbol
declare const __name: unique symbol
declare const __nonNull: unique symbol
declare const __reference: unique symbol

type Brand<
	Base,
	Name extends string
> = {
	[__base]: Base
	[__name]: Name
}

type GraphQLBrand<
	Base,
	Name extends string,
	NonNull extends boolean = false,
	Reference extends boolean = false
> = {
	[__base]: Base
	[__name]: Name
	[__nonNull]: NonNull
	[__reference]: Reference
}

export type JustBrand = Brand<any, string>
export type AnyGraphQLBrand = GraphQLBrand<any, string, boolean, boolean>

export type Branded<
	Base,
	Name extends string,
> = Base & Brand<Base, Name>

export type GraphQLBranded<
	Base,
	Name extends string,
	NonNull extends boolean = false,
	Reference extends boolean = false
> = Base & GraphQLBrand<Base, Name, NonNull, Reference>

export type GetBase<T> = T extends Brand<infer Base, any> ? Base : never
export type GetName<T> = T extends Brand<any, infer Name> ? Name : never

// export type GetBase<T> = T extends GraphQLBrand<infer Base, any, any, any> ? Base : never
// export type GetName<T> = T extends GraphQLBrand<any, infer Name, any, any> ? Name : never

export type GetNonNull<T> = T extends GraphQLBrand<any, any, infer NonNull, any> ? NonNull : never
export type GetReference<T> = T extends GraphQLBrand<any, any, any, infer Reference> ? Reference : never

export declare type NonNull<T> = GraphQLBranded<GetBase<T>, GetName<T>, true, GetReference<T>>
export declare type Reference<T> = GraphQLBranded<GetBase<T>, GetName<T>, GetNonNull<T>, true>

// export type BrandBuilder<T extends Branded<Base, any>, Base = BrandBase<T>> = {
// 	check: (value: Base) => value is T
// 	assert: (value: Base) => asserts value is T
// 	from: (value: Base) => T
// }

// export type BrandBuilderOptions<Base> = {
// 	validate?: (value: Base) => boolean | string
// }
