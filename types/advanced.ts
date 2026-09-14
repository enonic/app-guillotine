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
	Brand,
	BrandGraphQLObjectType,
	BrandGraphQLScalarType,
	GetParsedJson,
	GetNonNull,
	GetReturnType,
	GetSuperType,
	GetTypeName,
	GetReference,
	NonNull,
	Reference,
} from './brand'

export type {
	CreationCallback,
	CreationCallbacks,
	DataFetchingEnvironment,
	Extensions,
	Resolver,
	Resolvers,
	Type,
	Types,
} from './extensions'

export type {
	CreateDataFetcherResult,
	CreateDataFetcherResultParams,
	DataFetcherResult,
	GraphQL,
	GraphQLArgs,
	GraphQLBaseScalars,
	GraphQLBoolean,
	GraphQLCustomScalars,
	GraphQLEnumTypeReference,
	GraphQLDate,
	GraphQLDateTime,
	GraphQLExtendedScalars,
	GraphQLFloat,
	GraphQLID,
	GraphQLInputType,
	GraphQLInputTypeReference,
	GraphQLInputTypeName,
	GraphQLInt,
	GraphQLInterfaceTypeReference,
	GraphQLJson,
	GraphQLLocalDateTime,
	GraphQLLocalTime,
	GraphQLNonNull,
	GraphQLObjectType,
	GraphQLObjectTypeName,
	GraphQLObjectTypeReference,
	GraphQLScalars,
	GraphQLString,
	GraphQLTypeToGuillotineFields,
	GraphQLTypeToResolverResult,
	GraphQLUnionTypeReference,
	LocalContext,
	LocalContextRecord,
} from './graphQL'

export type {
	AccessControlEntry,
	Attachment,
	AttachmentUrl,
	EnumTypeNames,
	InputTypeNames,
	ObjectTypeNames,
	ScalarTypeNames,
	Component,
	Content,
	ContentConnection,
	ContentEdge,
	ContentType,
	FormInput,
	FormItem,
	FormItemSet,
	FormLayout,
	FormOptionSet,
	FormOptionSetOption,
	FragmentComponentData,
	GeoPoint,
	HeadlessCms,
	Icon,
	Image,
	ImageComponentData,
	ImageStyle,
	ImageUrl,
	LayoutComponentData,
	Link,
	Macro,
	Media,
	media_Image,
	media_Image_Data,
	MediaFocalPoint,
	MediaUploader,
	Mixin,
	Mixin_base_ApplicationConfig,
	Mixin_base_gpsInfo_DataConfig,
	Mixin_media_ApplicationConfig,
	Mixin_media_cameraInfo_DataConfig,
	Mixin_media_imageInfo_DataConfig,
	Occurrences,
	PageComponentData,
	PageInfo,
	PageUrl,
	PartComponentData,
	Permissions,
	portal_Site,
	portal_Site_Data,
	PrincipalKey,
	PublishInfo,
	Query,
	QueryDSLContentConnection,
	RichText,
	SiteConfigurator,
	TextComponentData,
	UntypedContent,
} from './guillotine'

export type {
	BaseFolderContent,
	MediaImageContent,
	PortalSiteContent,
} from './xp'


export {
	EnumTypeName,
	InputTypeName,
	ObjectTypeName,
	ScalarTypeName,
	ComponentType,
	FormItemType,
	MediaIntentType,
	Permission,
	PrincipalType,
} from './guillotine'


export {
	jsonParse,
	jsonStringify,
} from './brand'
