import type {
	NonNull,
} from './brand';
import type {
// 	GraphQLBoolean,
// 	GraphQLDate,
// 	GraphQLDateTime,
	GraphQLFloat,
	GraphQLID,
// 	GraphQLInt,
// 	GraphQLJson,
// 	GraphQLLocalDateTime,
// 	GraphQLLocalTime,
	GraphQLString,
} from './graphQL/ScalarTypes';
import {
	GuillotineBuiltinObjectTypeName,
	GuillotineContent,
	Guillotinemedia_Image,
	Guillotineportal_Site,
} from './guillotine/objectTypes';
// import {GuillotineBuiltinScalarTypeName} from './guillotine/ScalarTypes';


declare global {
	interface GraphQLEnumTypesMap {
		[enumTypeName: string]: unknown
	}
	interface GraphQLInputTypeFieldsMap {
		[inputTypeName: string]: {
			[inputTypeFieldName: string]: unknown // TODO GraphQLScalars | GraphQLReference<GraphQLInputType>
		}
	}
	interface GraphQLInputTypesMap {
		[inputTypeName: string]: unknown
	}
	interface GraphQLInterfaceTypesMap {
		[interfaceTypeName: string]: unknown
	}
	interface GraphQLObjectTypeFieldsMap {
		[objectTypeName: string]: {
			[objectTypeFieldName: string]: unknown // TODO GraphQLScalars | GraphQLReference<GraphQLObjectType>
		}
	}
	interface GraphQLObjectTypesMap {
		[GuillotineBuiltinObjectTypeName.Content]: GuillotineContent
		[GuillotineBuiltinObjectTypeName.media_Image]: Guillotinemedia_Image
		[GuillotineBuiltinObjectTypeName.portal_Site]: Guillotineportal_Site
		[typeName: string]: unknown
	}
	// interface GraphQLScalarTypesMap {
	// 	[GuillotineBuiltinScalarTypeName.Boolean]: GraphQLBoolean
	// 	[GuillotineBuiltinScalarTypeName.Date]: GraphQLDate
	// 	[GuillotineBuiltinScalarTypeName.DateTime]: GraphQLDateTime
	// 	[GuillotineBuiltinScalarTypeName.Float]: GraphQLFloat
	// 	[GuillotineBuiltinScalarTypeName.ID]: GraphQLID
	// 	[GuillotineBuiltinScalarTypeName.Json]: GraphQLJson
	// 	[GuillotineBuiltinScalarTypeName.Int]: GraphQLInt
	// 	[GuillotineBuiltinScalarTypeName.LocalDateTime]: GraphQLLocalDateTime
	// 	[GuillotineBuiltinScalarTypeName.LocalTime]: GraphQLLocalTime
	// 	[GuillotineBuiltinScalarTypeName.String]: GraphQLString
	// }
	interface GraphQLUnionTypesMap {
		[unionTypeName: string]: unknown
	}
} // global


export {}
