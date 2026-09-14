/// <reference types="@enonic-types/global"/>


import type {ScriptValue} from '@enonic-types/core'
import type {
	LocalContext,
	LocalContextRecord,
} from './LocalContext'


declare const __dataFetcherResult: unique symbol


export declare interface CreateDataFetcherResultParams<
	In extends LocalContextRecord = LocalContextRecord,
	Out extends LocalContextRecord = LocalContextRecord
> {
	// Objects and arrays must be wrapped with __.toScriptValue; primitives may be passed as is.
	data: ScriptValue | string | number | boolean
	localContext?: Out
	parentLocalContext?: LocalContext<In>
}

// Opaque value returned by createDataFetcherResult.
// Return it from a resolver as is; Guillotine unwraps data and localContext.
export declare interface DataFetcherResult {
	readonly [__dataFetcherResult]: true
}

export declare type CreateDataFetcherResult = <
	In extends LocalContextRecord = LocalContextRecord,
	Out extends LocalContextRecord = LocalContextRecord
>(params: CreateDataFetcherResultParams<In,Out>) => DataFetcherResult
