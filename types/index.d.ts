/*

There are several "sets" of types when working with Guillotine Extensions:

1. The types that are used in the GraphQL schema definition.
2. The types that are passed into resolvers via the source parameter.
3. The types that are returned from resolvers.

All these types are related, but they are not the same, and conversion between is sketchy.
Especically when the GraphQL schema just uses JSON, but returns a complex object.

A resolver can return:

1. a nested data set
2. a flat data set, and let other resolvers handle the nested data
3. a combination of both

In resolvers the type of source does NOT match the GraphQLSchema.
It seems to match Enonic "Core" types with some extensions.
For example "attachments" currently have a "__contentId" field that will be removed in Guillotine 8.

In "nested" resolvers the type of source depends on what the parent resolver returns.

In resolvers the type of localContext depends on a combination of all ancestor resolvers.

*/

/// <reference path="./Global.Modifying.d.ts"/>


export type {
	Branded,
	GraphQLBranded,
} from './Branded'

export type {
	CreationCallback,
	CreationCallbacks,
	DataFetchingEnvironment,
	Extensions,
	LocalContext,
	LocalContextRecord,
	Resolver,
	Resolvers,
	Type,
	Types,
} from './extensions/'

export type {
	CreateDataFetcherResult,
	CreateDataFetcherResultParams,
	GraphQL,
	GraphQLArgs,
	GraphQLBaseScalars,
	GraphQLBoolean,
	GraphQLCustomScalars,
	GraphQLDate,
	GraphQLDateTime,
	GraphQLExtendedScalars,
	GraphQLFloat,
	GraphQLID,
	GraphQLInputType,
	GraphQLInputTypeName,
	GraphQLInt,
	GraphQLJson,
	GraphQLLocalDateTime,
	GraphQLLocalTime,
	GraphQLNonNull,
	GraphQLObjectType,
	GraphQLObjectTypeName,
	GraphQLReference,
	GraphQLScalars,
	GraphQLString,
	GraphQLTypeToGuillotineFields,
	GraphQLTypeToResolverResult,
} from './graphQL/'

export type {
	GuillotineAccessControlEntry,
	GuillotineAttachment,
	GuillotineBuiltinEnumTypeName,
	GuillotineBuiltinEnumTypeNames,
	GuillotineBuiltinInputTypeName,
	GuillotineBuiltinInputTypeNames,
	GuillotineBuiltinObjectTypeName,
	GuillotineBuiltinObjectTypeNames,
	GuillotineBuiltinScalarTypeName,
	GuillotineBuiltinScalarTypeNames,
	GuillotineContent,
	GuillotineContentConnection,
	GuillotineContentEdge,
	GuillotineContentType,
	GuillotineExtraData,
	GuillotineFormItem,
	GuillotineFormItemType, // enum
	GuillotineGeoPoint,
	GuillotineIcon,
	Guillotinemedia_Image,
	Guillotinemedia_Image_Data,
	GuillotineMediaFocalPoint,
	GuillotineMediaUploader,
	GuillotinePageInfo,
	GuillotinePermission, // enum
	GuillotinePermissions,
	Guillotineportal_Site,
	Guillotineportal_Site_Data,
	GuillotinePrincipalKey,
	GuillotinePrincipalType, // enum
	GuillotinePublishInfo,
	GuillotineXData_base_ApplicationConfig,
	GuillotineXData_base_gpsInfo_DataConfig,
	GuillotineXData_media_ApplicationConfig,
	GuillotineXData_media_cameraInfo_DataConfig,
	GuillotineXData_media_imageInfo_DataConfig,
} from './guillotine/'
