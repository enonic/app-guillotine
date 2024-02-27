import type {GraphQLObjectTypeName} from '../graphQL/ObjectTypes'


// Unions can only reference ObjectTypes.
// An ObjectType can implement zero or more Interfaces.
export declare type TypeResolver = (param: any) => GraphQLObjectTypeName



export declare type TypeResolvers = Record<string, TypeResolver>
