export declare interface Enum {
	description: string
	values: Record<string, string>
}

export declare type Enums = Record<
	string, // Could make a global map and use GraphQLEnumTypeName
	Enum
>
