import type {
	NonNull,
} from './Branded';
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
} from './GraphQL';
import {
	GuillotineBuiltinObjectTypeName
} from './ObjectTypes';
import {
	GraphQLContent,
	GraphQLmedia_Image,
} from './Schema';


declare global {
	interface GraphQLInputTypesMap {
		[inputTypeName: string]: unknown
	}
	interface GraphQLObjectTypesMap {
		// Date: GraphQLDate
		// DateTime: GraphQLDateTime
		// Json: GraphQLJson
		// GraphQLBoolean: GraphQLBoolean
		// GraphQLFloat: GraphQLFloat
		// GraphQLID: GraphQLID
		// GraphQLInt: GraphQLInt
		// GraphQLString: GraphQLString
		// LocalDateTime: GraphQLLocalDateTime
		// LocalTime: GraphQLLocalTime
		[GuillotineBuiltinObjectTypeName.Content]: GraphQLContent
		[GuillotineBuiltinObjectTypeName.media_Image]: GraphQLmedia_Image
		[typeName: string]: unknown
	}
	interface GraphQLObjectTypeFieldsMap {
		// [typeName: string]: string[]
		// [GuillotineObjectTypeName.Content]: {
		// 	_id: NonNull<GraphQLID>
		// 	_name: NonNull<GraphQLString>
		// 	_path: NonNull<GraphQLString>
		// 	//_references: GraphQLContent[]
		// 	_score: GraphQLFloat
		// 	creator: PrincipalKey
		// 	modifier: PrincipalKey
		// 	createdTime: GraphQLDateTime
		// 	modifiedTime: GraphQLDateTime
		// 	owner: PrincipalKey
		// 	type: String
		// 	contentType: ContentType
		// 	displayName: String
		// 	hasChildren: Boolean
		// 	language: String
		// 	valid: Boolean
		// 	dataAsJson: JSON
		// 	x: ExtraData
		// 	xAsJson: JSON
		// 	pageAsJson: JSON
		// 	pageTemplate: Content
		// 	components: [Component]
		// 	attachments: [Attachment]
		// 	publish: PublishInfo
		// 	pageUrl: String
		// 	site: portal_Site
		// 	parent: Content
		// 	children: [Content]
		// 	childrenConnection: ContentConnection
		// 	permissions: Permissions
		// }
		[typeName: string]: {
			[fieldName: string]: unknown // TODO
		}
	}
} // global


export {}
