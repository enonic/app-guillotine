export declare type ArrayElement<ArrayType extends readonly unknown[]> =
	ArrayType extends readonly (infer ElementType)[] ? ElementType : never

export declare type JSON =
	| string // double-quoted
	| number
	| boolean
	| null
	| { [key: string]: JSON }
	// | Record<string, JSON> // Causes: Type alias JSON circularly references itself.
	| JSON[]

export declare type PartialRecord<K extends keyof any, T> = {
	[P in K]?: T
}

export declare type ValueOf<T> = T[keyof T]
