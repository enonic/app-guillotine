import type {Content as CoreContent} from '@enonic-types/core';
import type {
	GraphQLBoolean,
	GraphQLFloat,
	GraphQLInt,
	GraphQLObjectTypeReference,
	GraphQLString,
} from '../advanced';
import type {
	GetSuperType,
} from '../brand';
import {
	GraphQLTypeToGuillotineFields,
	GraphQLTypeToResolverResult,
} from '../graphQL/';
import type {Content} from '../guillotine/objectTypes'


import {
	expectAssignable,
	expectNotAssignable,
	expectType,
} from 'tsd';


expectAssignable<boolean>(<GraphQLBoolean>true);
expectNotAssignable<boolean>('string');
expectType<boolean>(<GetSuperType<GraphQLBoolean>>true);

expectAssignable<string>(<GraphQLString>'string');
expectAssignable<GraphQLString>(<GraphQLString>'string');
expectAssignable<GraphQLString>('string' as GraphQLString);
expectType<string>(<GetSuperType<GraphQLString>>'string');


//──────────────────────────────────────────────────────────────────────────────
// Converters
//──────────────────────────────────────────────────────────────────────────────
declare global {
	interface GraphQLObjectTypesMap {
		MyType: MyGraphQLType
	}
}

interface MyGraphQLType {
	boolean: GraphQLBoolean
	booleanArray: GraphQLBoolean[]
	float: GraphQLFloat
	floatArray: GraphQLFloat[]
	int: GraphQLInt
	intArray: GraphQLInt[]
	reference: GraphQLObjectTypeReference<Content>
	string: GraphQLString
	stringArray: GraphQLString[]
}

const myContent: CoreContent = {
	_id: '_id',
	_name: '_name',
	_path: '_path',
	attachments: {},
	creator: 'user:system:john',
	createdTime: 'createdTime',
	data: {},
	displayName: 'displayName',
	hasChildren: true,
	owner: 'user:system:john',
	type: 'base:folder',
	valid: true,
	x: {}
}

//──────────────────────────────────────────────────────────────────────────────
// GraphQLTypeToGuillotineFields
//──────────────────────────────────────────────────────────────────────────────
const fields: GraphQLTypeToGuillotineFields<MyGraphQLType> = {
	boolean: {
		type: <GraphQLBoolean>true
	},
	booleanArray: {
		type: <GraphQLBoolean[]>[true,false]
	},
	float: {
		type: <GraphQLFloat>0.1
	},
	floatArray: {
		type: <GraphQLFloat[]>[0.1,0.2]
	},
	int: {
		type: <GraphQLInt>0
	},
	intArray: {
		type: <GraphQLInt[]>[0,1,2]
	},
	reference: {
		type: <GraphQLObjectTypeReference<Content>>{}
	},
	string: {
		type: <GraphQLString>'a'
	},
	stringArray: {
		type: <GraphQLString[]>['a', 'b']
	}
}

expectType<{
	boolean: {
		type: GraphQLBoolean
	}
	booleanArray: {
		type: GraphQLBoolean[]
	}
	float: {
		type: GraphQLFloat
	}
	floatArray: {
		type: GraphQLFloat[]
	}
	int: {
		type: GraphQLInt
	}
	intArray: {
		type: GraphQLInt[]
	}
	reference: {
		type: GraphQLObjectTypeReference<Content>
	}
	string: {
		type: GraphQLString
	}
	stringArray: {
		type: GraphQLString[]
	}
}>(fields);


//──────────────────────────────────────────────────────────────────────────────
// GraphQLTypeToResolverResult
//──────────────────────────────────────────────────────────────────────────────
expectType<boolean>(<GraphQLTypeToResolverResult<GraphQLBoolean>>true);
expectType<number>(<GraphQLTypeToResolverResult<GraphQLFloat>>0.1);
expectType<number>(<GraphQLTypeToResolverResult<GraphQLInt>>0);
expectType<string>(<GraphQLTypeToResolverResult<GraphQLString>>'string');
expectType<boolean[]>(<GraphQLTypeToResolverResult<GraphQLBoolean[]>>[true]);
expectType<number[]>(<GraphQLTypeToResolverResult<GraphQLFloat[]>>[0.1]);
expectType<number[]>(<GraphQLTypeToResolverResult<GraphQLInt[]>>[0]);
expectType<string[]>(<GraphQLTypeToResolverResult<GraphQLString[]>>['string']);


const resolverResult: GraphQLTypeToResolverResult<MyGraphQLType> = {
	boolean: true,
	booleanArray: [true,false],
	float: 0.1,
	floatArray: [0.1],
	int: 0,
	intArray: [0,1],
	reference: myContent,
	string: 'a',
	stringArray: ['a', 'b'],
};

expectType<{
	boolean: boolean
	booleanArray: boolean[]
	float: number
	floatArray: number[]
	int: number
	intArray: number[]
	reference: CoreContent
	string: string
	stringArray: string[]
}>(resolverResult)
