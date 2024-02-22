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
} from './graphQL/ScalarTypes';
import {
	GuillotineBuiltinObjectTypeName,
	GuillotineContent,
	Guillotinemedia_Image,
} from './guillotine/ObjectTypes';
// import {GuillotineBuiltinScalarTypeName} from './guillotine/ScalarTypes';


declare global {
	interface GraphQLInputTypesMap {
		[inputTypeName: string]: unknown
	}
	interface GraphQLObjectTypesMap {
		[GuillotineBuiltinObjectTypeName.Content]: GuillotineContent
		[GuillotineBuiltinObjectTypeName.media_Image]: Guillotinemedia_Image
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
		// 	dataAsJson: ParsedJSON
		// 	x: ExtraData
		// 	xAsJson: ParsedJSON
		// 	pageAsJson: ParsedJSON
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
} // global


export {}
