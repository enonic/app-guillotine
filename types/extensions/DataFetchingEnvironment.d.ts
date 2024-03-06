import type {
	LocalContext,
	LocalContextRecord
} from '../graphQL/LocalContext'


export declare interface DataFetchingEnvironment<
	ARGS extends Record<string, any> = Record<string, any>,
	LOCAL_CONTEXT extends LocalContextRecord = LocalContextRecord,
	SOURCE = any,
> {
	args: ARGS
	localContext: LocalContext<LOCAL_CONTEXT>
	source: SOURCE
}
