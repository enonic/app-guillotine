export declare type ArrayElement<ArrayType extends readonly unknown[]> =
	ArrayType extends readonly (infer ElementType)[] ? ElementType : never

export declare type ParsedJSON =
	| string // double-quoted inside the JSON string, but normal string after parsing
	| number
	| boolean
	| null
	// | { [key: string]: any } // This works both for types and interfaces :)
	| Record<string, any> // This works both for types and interfaces :)
	// | { [key: string]: unknown } // This works for types, but NOT interfaces :(
	// | Record<string, unknown> // This works for types, but NOT interfaces :(
	// | { [key: string]: ParsedJSON } // This works for types, but NOT interfaces :(
	// | Record<string, ParsedJSON> // Causes: Type alias ParsedJSON circularly references itself. :(
	| ParsedJSON[]

export declare type PartialRecord<K extends keyof any, T> = {
	[P in K]?: T
}

export declare type ValueOf<T> = T[keyof T]
