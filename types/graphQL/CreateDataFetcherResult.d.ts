/// <reference types="@enonic-types/global"/>


import type {ScriptValue} from '@enonic-types/core'
import type {
	LocalContext,
	LocalContextRecord,
} from './LocalContext'


export declare interface CreateDataFetcherResultParams<
	In extends LocalContextRecord = LocalContext,
	Out extends LocalContextRecord = LocalContext
> {
	data: ScriptValue
	localContext?: LocalContext<Out>
	parentLocalContext?: LocalContext<In>
}

export declare type CreateDataFetcherResult = <
	In extends LocalContextRecord = LocalContext,
	Out extends LocalContextRecord = LocalContext
>(params: CreateDataFetcherResultParams<In,Out>) => LocalContext<In&Out>
