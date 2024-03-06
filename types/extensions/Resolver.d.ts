import type {LocalContextRecord} from '../graphQL/LocalContext'
import type {DataFetchingEnvironment} from './DataFetchingEnvironment'


export declare interface Resolver<
	ARGS extends Record<string, any> = Record<string, any>,
	LOCAL_CONTEXT extends LocalContextRecord = LocalContextRecord,
	SOURCE = any,
	RETURN_TYPE = any
> {
	(env: DataFetchingEnvironment<ARGS,LOCAL_CONTEXT,SOURCE>): RETURN_TYPE
}
