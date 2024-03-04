export declare type LocalContextRecord = Record<string,string|number|boolean|null>


// The documentation:
// https://developer.enonic.com/docs/guillotine/stable/extending/resolvers
//
// Says:
// If parentLocalContext is not provided, then localContext will override
// parentLocalContext.
//
// This means that is possible to remove branch, project and siteKey in some
// parent createDataFetcherResult. Which is why the Partial<> is used.
export declare type LocalContext<
	T extends LocalContextRecord = LocalContextRecord
> = Partial<{
	branch: string
	project: string
	siteKey?: string
}> & T
