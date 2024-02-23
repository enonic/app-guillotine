import type {BrandGraphQLScalarType} from '../brand'


export declare type GraphQLBoolean = BrandGraphQLScalarType<'GraphQLBoolean', boolean>
export declare type GraphQLDate = BrandGraphQLScalarType<'Date', string>
export declare type GraphQLDateTime = BrandGraphQLScalarType<'DateTime', string>
export declare type GraphQLFloat = BrandGraphQLScalarType<'GraphQLFloat', number>
export declare type GraphQLID = BrandGraphQLScalarType<'GraphQLID', string>
export declare type GraphQLInt = BrandGraphQLScalarType<'GraphQLInt', number>
export declare type GraphQLJson = BrandGraphQLScalarType<'Json', string>
export declare type GraphQLLocalDateTime = BrandGraphQLScalarType<'LocalDateTime', string>
export declare type GraphQLLocalTime = BrandGraphQLScalarType<'LocalTime', string>
export declare type GraphQLString = BrandGraphQLScalarType<'GraphQLString', string>

export declare interface GraphQLBaseScalars {
	GraphQLBoolean: GraphQLBoolean
	GraphQLFloat: GraphQLFloat
	GraphQLID: GraphQLID
	GraphQLInt: GraphQLInt
	GraphQLString: GraphQLString
}

export declare interface GraphQLExtendedScalars {
	Date: GraphQLDate
	DateTime: GraphQLDateTime
	Json: GraphQLJson
}

export declare interface GraphQLCustomScalars {
	LocalDateTime: GraphQLLocalDateTime
	LocalTime: GraphQLLocalTime
}

export declare interface GraphQLScalars
	extends GraphQLBaseScalars, GraphQLExtendedScalars, GraphQLCustomScalars {}
