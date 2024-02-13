export declare type LocalContextRecord = Record<string,string|number|boolean|null>

export declare type LocalContext<
	T extends LocalContextRecord = LocalContextRecord
> = {
	branch: string
	project: string
	siteKey?: string
} & T
