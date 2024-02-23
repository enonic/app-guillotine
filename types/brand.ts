import type {ParsedJSON} from './Utils'

//──────────────────────────────────────────────────────────────────────────────
// Unique symbols (only available in this file)
//──────────────────────────────────────────────────────────────────────────────
declare const __superType: unique symbol
declare const __name: unique symbol
declare const __parsedJsonType: unique symbol

// GraphQL related
declare const __nonNull: unique symbol
declare const __reference: unique symbol
declare const __returnType: unique symbol


//──────────────────────────────────────────────────────────────────────────────
// Non-exported types
//──────────────────────────────────────────────────────────────────────────────

// WARNING: Do NOT export this type, it uses symbols only available in this file.
declare interface MetaObject<
	TYPE_NAME extends string, // All branded types need a unique name to be distinguisable in discriminated unions.
	SUPER_TYPE extends any,
	PARSED_JSON_TYPE extends ParsedJSON|undefined = undefined
> {
	[__name]: TYPE_NAME
	[__parsedJsonType]: PARSED_JSON_TYPE
	[__superType]: SUPER_TYPE
}

// WARNING: Do NOT export this type, it uses symbols only available in this file.
declare interface GraphQLMetaObject<
	TYPE_NAME extends string,
	SUPER_TYPE extends any,
	PARSED_JSON_TYPE extends ParsedJSON|undefined = undefined,
	NON_NULL extends boolean = false, // Both Objects and Scalars can be non-null
	RETURN_TYPE extends any = undefined, // Only Objects have a return type
	REFERENCE extends boolean = false, // Only Objects can be referenced to
> extends MetaObject<TYPE_NAME, SUPER_TYPE, PARSED_JSON_TYPE> {
	[__nonNull]: NON_NULL
	[__reference]: REFERENCE
	[__returnType]: RETURN_TYPE
}

// WARNING: Do NOT export this type, BrandGraphQLObjectType and BrandGraphQLScalarType should be used instead.
declare type BrandGraphQL<
	TYPE_NAME extends string,
	SUPER_TYPE extends any,
	PARSED_JSON_TYPE extends ParsedJSON|undefined = undefined,
	NON_NULL extends boolean = false,
	RETURN_TYPE extends any = undefined,
	REFERENCE extends boolean = false
> = SUPER_TYPE & GraphQLMetaObject<
	TYPE_NAME,        // 1. Type name
	SUPER_TYPE,       // 2. Super type
	PARSED_JSON_TYPE, // 3. Parsed JSON type
	NON_NULL,         // 4. Non-null
	RETURN_TYPE,      // 5. Return type
	REFERENCE         // 6. Reference
>


//──────────────────────────────────────────────────────────────────────────────
// Exported types
//──────────────────────────────────────────────────────────────────────────────
export type AnyBrand = MetaObject<
	string, // 1. Type name
	any,    // 2. Super type
	any     // 3. Parsed JSON type
>

// export type AnyJson = MetaObject<any, string, ParsedJSON>

export type AnyGraphQLBrand = GraphQLMetaObject<
	string,  // 1. Type name
	any,     // 2. Super type
	any,     // 3. Parsed JSON type
	boolean, // 4. Non-null
	any,     // 5. Return type
	boolean  // 6. Reference
>

export declare type Brand<
	TYPE_NAME extends string,
	SUPER_TYPE,
	PARSED_JSON_TYPE extends ParsedJSON|undefined = undefined
> = SUPER_TYPE & MetaObject<
	TYPE_NAME,       // 1. Type name
	SUPER_TYPE,      // 2. Super type
	PARSED_JSON_TYPE // 3. Parsed JSON type
>

export declare type GetTypeName<T> = T extends MetaObject<
	infer TYPE_NAME, // 1. Type name
	any,             // 2. Super type
	any              // 3. Parsed JSON type
> ? TYPE_NAME : never

export declare type GetSuperType<T> = T extends MetaObject<
	any,              // 1. Type name
	infer SUPER_TYPE, // 2. Super type
	any               // 3. Parsed JSON type
> ? SUPER_TYPE : never

export declare type GetParsedJson<T> = T extends MetaObject<
	any,                   // 1. Type name
	any,                   // 2. Super type
	infer PARSED_JSON_TYPE // 3. Parsed JSON type
> ? PARSED_JSON_TYPE : never

export declare type GetNonNull<T> = T extends GraphQLMetaObject<
	string,         // 1. Type name
	any,            // 2. Super type
	any,            // 3. Parsed JSON type
	infer NON_NULL, // 4. Non-null
	any,            // 5. Return type
	any             // 6. Reference
> ? NON_NULL : never

export declare type GetReturnType<T> = T extends GraphQLMetaObject<
	string,            // 1. Type name
	any,               // 2. Super type
	any,               // 3. Parsed JSON type
	any,               // 4. Non-null
	infer RETURN_TYPE, // 5. Return type
	any                // 6. Reference
> ? RETURN_TYPE : never

export declare type GetReference<T> = T extends GraphQLMetaObject<
	string,         // 1. Type name
	any,            // 2. Super type
	any,            // 3. Parsed JSON type
	any,            // 4. Non-null
	any,            // 5. Return type
	infer REFERENCE // 6. Reference
> ? REFERENCE : never

export declare type NonNull<T> = BrandGraphQL<
	GetTypeName<T>,   // 1. Type name
	GetSuperType<T>,  // 2. Super type
	GetParsedJson<T>, // 3. Parsed JSON type
	true,             // 4. Non-null
	GetReturnType<T>, // 5. Return type
	GetReference<T>   // 6. Reference
>

export declare type Reference<T> = BrandGraphQL<
	GetTypeName<T>,   // 1. Type name
	GetSuperType<T>,  // 2. Super type
	GetParsedJson<T>, // 3. Parsed JSON type
	GetNonNull<T>,    // 4. Non-null
	GetReturnType<T>, // 5. Return type
	true              // 6. Reference
>

// NonNull and Reference can be applied via wrapping.
export declare type BrandGraphQLObjectType<
	GQL_OBJECT_TYPE_NAME extends string,
	GQL_OBJECT_TYPE extends Record<string, any>,
	RETURN_TYPE extends any = undefined,
> = BrandGraphQL<
	GQL_OBJECT_TYPE_NAME, // 1. Type name
	GQL_OBJECT_TYPE,      // 2. Super type
	undefined,            // 3. Parsed JSON type
	false,                // 4. Non-null
	RETURN_TYPE,          // 5. Return type
	false                 // 6. Reference
>

// NonNull can be applied via wrapping.
export declare type BrandGraphQLScalarType<
	GQL_SCALAR_TYPE_NAME extends string,
	SCALAR_TYPE extends boolean|string|number
> = BrandGraphQL<
	GQL_SCALAR_TYPE_NAME, // 1. Type name
	SCALAR_TYPE,          // 2. Super type
	undefined,            // 3. Parsed JSON type
	false,                // 4. Non-null
	undefined,            // 5. Return type
	false                 // 6. Reference
>


//──────────────────────────────────────────────────────────────────────────────
// Exported functions
//──────────────────────────────────────────────────────────────────────────────
export function jsonParse<T extends string>(json: T): GetParsedJson<T> {
	return JSON.parse(json) as GetParsedJson<T>;
}

export function jsonStringify<T>(value: unknown): T {
	return JSON.stringify(value) as T;
}
