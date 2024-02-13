// Make sure ScriptValue type exists in global scope:
/// <reference types="@enonic-types/global"/>


import type {
	LocalContext,
	LocalContextRecord,
} from './LocalContext'


export declare interface CreateDataFetcherResultParams<
	In extends LocalContextRecord = LocalContext,
	Out extends LocalContextRecord = LocalContext
> {
	data: ScriptValue // NOTE: ScriptValue type is expected to exist in global scope
	localContext?: LocalContext<Out>
	parentLocalContext?: LocalContext<In>
}

export declare type CreateDataFetcherResult = <
	In extends LocalContextRecord = LocalContext,
	Out extends LocalContextRecord = LocalContext
>(params: CreateDataFetcherResultParams<In,Out>) => LocalContext<In&Out>
