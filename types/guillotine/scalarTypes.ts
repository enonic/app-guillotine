// This is a value, do not add declare.
export enum ScalarTypeName {
	Boolean = 'Boolean',
	Date = 'Date',
	DateTime = 'DateTime',
	Float = 'Float',
	ID = 'ID',
	Int = 'Int',
	Json = 'Json',
	LocalDateTime = 'LocalDateTime',
	LocalTime = 'LocalTime',
	String = 'String',
}

export declare type ScalarTypeNames = keyof typeof ScalarTypeName
