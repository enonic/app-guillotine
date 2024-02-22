import type {GraphQLBranded} from '../Branded'


export declare type GraphQLBoolean = GraphQLBranded<boolean, 'GraphQLBoolean'>
export declare type GraphQLDate = GraphQLBranded<string, 'Date'>
export declare type GraphQLDateTime = GraphQLBranded<string, 'DateTime'>
export declare type GraphQLFloat = GraphQLBranded<number, 'GraphQLFloat'>
export declare type GraphQLID = GraphQLBranded<string, 'GraphQLID'>
export declare type GraphQLInt = GraphQLBranded<number, 'GraphQLInt'>
export declare type GraphQLJson = GraphQLBranded<string, 'Json'>
export declare type GraphQLLocalDateTime = GraphQLBranded<string, 'LocalDateTime'>
export declare type GraphQLLocalTime = GraphQLBranded<string, 'LocalTime'>
export declare type GraphQLString = GraphQLBranded<string, 'GraphQLString'>

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
