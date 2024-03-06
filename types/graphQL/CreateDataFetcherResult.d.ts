// Make sure ScriptValue type exists in global scope:
/// <reference types="@enonic-types/global"/>


import type {
	LocalContext,
	LocalContextRecord,
} from './LocalContext'


// https://developer.enonic.com/docs/guillotine/stable/extending/resolvers#createdatafetcherresult
//
// If parentLocalContext is not provided, then localContext will override
// parentLocalContext.
//
// Otherwise, localContext will be merged with parentLocalContext and
// localContext will override parentLocalContext if they have the same keys.
//
// If localContext is not provided, then parentLocalContext will be used as
// localContext.


export declare interface CreateDataFetcherResultParams<
	DATA extends any,
	LOCAL_CONTEXT extends LocalContextRecord = LocalContext,
	PARENT_LOCAL_CONTEXT extends LocalContextRecord = LocalContext
> {
	data: ScriptValue // NOTE: ScriptValue type is expected to exist in global scope
	localContext?: LocalContext<LOCAL_CONTEXT>
	parentLocalContext?: LocalContext<PARENT_LOCAL_CONTEXT>
}

// NOTE: Same as DataFetchingEnvironment without args.
export interface DataFetcherResult<
	DATA extends any,
	LOCAL_CONTEXT extends LocalContextRecord = LocalContext
> {
	// args, // Depends on the field resolver
	localContext: LocalContext<LOCAL_CONTEXT>
	source: DATA,
}

export declare type CreateDataFetcherResult = <
	DATA extends any,
	LOCAL_CONTEXT extends LocalContextRecord = LocalContext,
	PARENT_LOCAL_CONTEXT extends LocalContextRecord = LocalContext
>(params: CreateDataFetcherResultParams<
	DATA,
	LOCAL_CONTEXT,
	PARENT_LOCAL_CONTEXT
>) => DataFetcherResult<
	DATA,
	LocalContext<PARENT_LOCAL_CONTEXT & LOCAL_CONTEXT>
>
