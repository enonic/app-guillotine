// import type {
// 	GraphQLBoolean,
// 	GraphQLDate,
// 	GraphQLDateTime,
// 	GraphQLFloat,
// 	GraphQLID,
// 	GraphQLInt,
// 	GraphQLJson,
// 	GraphQLLocalDateTime,
// 	GraphQLLocalTime,
// 	GraphQLString,
// } from './graphQL/ScalarTypes';
import {
	ObjectTypeName,
	Content,
	media_Image,
	portal_Site,
} from './guillotine/objectTypes';
// import {ScalarTypeName} from './guillotine/ScalarTypes';


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
		[ObjectTypeName.Content]: Content
		[ObjectTypeName.media_Image]: media_Image
		[ObjectTypeName.portal_Site]: portal_Site
		[typeName: string]: unknown
	}
	// interface GraphQLScalarTypesMap {
	// 	[ScalarTypeName.Boolean]: GraphQLBoolean
	// 	[ScalarTypeName.Date]: GraphQLDate
	// 	[ScalarTypeName.DateTime]: GraphQLDateTime
	// 	[ScalarTypeName.Float]: GraphQLFloat
	// 	[ScalarTypeName.ID]: GraphQLID
	// 	[ScalarTypeName.Json]: GraphQLJson
	// 	[ScalarTypeName.Int]: GraphQLInt
	// 	[ScalarTypeName.LocalDateTime]: GraphQLLocalDateTime
	// 	[ScalarTypeName.LocalTime]: GraphQLLocalTime
	// 	[ScalarTypeName.String]: GraphQLString
	// }
	interface GraphQLUnionTypesMap {
		[unionTypeName: string]: unknown
	}
} // global


export {}
