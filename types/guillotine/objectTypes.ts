import type {Content as CoreContent} from '@enonic-types/core';
import type {
	BrandGraphQLObjectType,
	NonNull
} from '../brand'
import type {
	FormItemType,
	Permission,
	PrincipalType,
} from './enumTypes'
import type {
	GraphQLBoolean,
	GraphQLDateTime,
	GraphQLFloat,
	GraphQLID,
	GraphQLInt,
	GraphQLJson,
	GraphQLLocalDateTime,
	GraphQLString,
} from '../graphQL/ScalarTypes'
import type {
	MediaImageContent,
	PortalSiteContent,
} from '../xp'


// This is a value, do not add declare.
export enum ObjectTypeName {
	AccessControlEntry = 'AccessControlEntry',
	Attachment = 'Attachment',
	base_Folder = 'base_Folder',
	base_Media = 'base_Media',
	base_Shortcut = 'base_Shortcut',
	base_Shortcut_Data = 'base_Shortcut_Data',
	base_Shortcut_Parameters = 'base_Shortcut_Parameters',
	base_Structured = 'base_Structured',
	base_Unstructured = 'base_Unstructured',
	Component = 'Component',
	Content = 'Content',
	ContentConnection = 'ContentConnection',
	ContentEdge = 'ContentEdge',
	ContentType = 'ContentType',
	DefaultValue = 'DefaultValue',
	ExtraData = 'ExtraData',
	FormItem = 'FormItem',
	FormItemSet = 'FormItemSet',
	FormLayout = 'FormLayout',
	FormOptionSet = 'FormOptionSet',
	FormOptionSetOption = 'FormOptionSetOption',
	FragmentComponentData = 'FragmentComponentData',
	GeoPoint = 'GeoPoint',
	HeadlessCms = 'HeadlessCms',
	Icon = 'Icon',
	Image = 'Image',
	ImageComponentData = 'ImageComponentData',
	ImageStyle = 'ImageStyle',
	LayoutComponentData = 'LayoutComponentData',
	LayoutComponentDataConfig = 'LayoutComponentDataConfig',
	Link = 'Link',
	Macro = 'Macro',
	Macro_system_disable_DataConfig = 'Macro_system_disable_DataConfig',
	Macro_system_embed_DataConfig = 'Macro_system_embed_DataConfig',
	MacroConfig = 'MacroConfig',
	Media = 'Media',
	MediaFocalPoint = 'MediaFocalPoint',
	MediaUploader = 'MediaUploader',
	media_Archive = 'media_Archive',
	media_Archive_Data = 'media_Archive_Data',
	media_Audio = 'media_Audio',
	media_Audio_Data = 'media_Audio_Data',
	media_Code = 'media_Code',
	media_Code_Data = 'media_Code_Data',
	media_Data = 'media_Data',
	media_Data_Data = 'media_Data_Data',
	media_Document = 'media_Document',
	media_Document_Data = 'media_Document_Data',
	media_Executable = 'media_Executable',
	media_Executable_Data = 'media_Executable_Data',
	media_Image = 'media_Image',
	media_Image_Data = 'media_Image_Data',
	media_Presentation = 'media_Presentation',
	media_Presentation_Data = 'media_Presentation_Data',
	media_Spreadsheet = 'media_Spreadsheet',
	media_Spreadsheet_Data = 'media_Spreadsheet_Data',
	media_Text = 'media_Text',
	media_Text_Data = 'media_Text_Data',
	media_Unknown = 'media_Unknown',
	media_Unknown_Data = 'media_Unknown_Data',
	media_Vector = 'media_Vector',
	media_Vector_Data = 'media_Vector_Data',
	media_Video = 'media_Video',
	media_Video_Data = 'media_Video_Data',
	// Menu = 'Menu', // Not builtin?
	// MenuItem = 'MenuItem', // Not builtin?
	// MetaFields = 'MetaFields', // Not builtin!
	Occurrences = 'Occurrences',
	PageComponentData = 'PageComponentData',
	PageComponentDataConfig = 'PageComponentDataConfig',
	PageInfo = 'PageInfo',
	PartComponentData = 'PartComponentData',
	PartComponentDataConfig = 'PartComponentDataConfig',
	Permissions = 'Permissions',
	portal_Fragment = 'portal_Fragment',
	portal_PageTemplate = 'portal_PageTemplate',
	portal_PageTemplate_Data = 'portal_PageTemplate_Data',
	portal_Site = 'portal_Site',
	portal_Site_Data = 'portal_Site_Data',
	portal_TemplateFolder = 'portal_TemplateFolder',
	PrincipalKey = 'PrincipalKey',
	PublishInfo = 'PublishInfo',
	QueryContentConnection = 'QueryContentConnection',
	QueryDSLContentConnection = 'QueryDSLContentConnection',
	RichText = 'RichText',
	// RobotsTxt = 'RobotsTxt', // Not builtin?
	SiteConfigurator = 'SiteConfigurator',
	TextComponentData = 'TextComponentData',
	UntypedContent = 'UntypedContent',
	XData_base_ApplicationConfig = 'XData_base_ApplicationConfig',
	XData_base_gpsInfo_DataConfig = 'XData_base_gpsInfo_DataConfig',
	XData_media_ApplicationConfig = 'XData_media_ApplicationConfig',
	XData_media_cameraInfo_DataConfig = 'XData_media_cameraInfo_DataConfig',
	XData_media_imageInfo_DataConfig = 'XData_media_imageInfo_DataConfig',
}

export declare type ObjectTypeNames = keyof typeof ObjectTypeName

//──────────────────────────────────────────────────────────────────────────────

export declare interface AccessControlEntry {
	principal: PrincipalKey
	allow: Permission[] // enum
	deny: Permission[] // enum
}

export declare interface Attachment {
	name: GraphQLString
	label: GraphQLString
	size: GraphQLInt
	mimeType: GraphQLString
	attachmentUrl: GraphQLString
}

export declare type Content<
	Extensions extends Record<string, unknown> = Record<string, unknown>
> = BrandGraphQLObjectType<
	'Content',
	{
		_id: NonNull<GraphQLID>
		_name: NonNull<GraphQLString>
		_path: NonNull<GraphQLString>
		_references: Content[]
		_score: GraphQLFloat
		attachments: Attachment[]
		children: Content[]
		childrenConnection: ContentConnection
		components: Content[]
		contentType: ContentType
		createdTime: GraphQLDateTime
		creator: PrincipalKey
		dataAsJson: GraphQLJson
		displayName: GraphQLString
		hasChildren: GraphQLBoolean
		language: GraphQLString
		modifiedTime: GraphQLDateTime
		modifier: PrincipalKey
		owner: PrincipalKey
		pageAsJson: GraphQLJson
		pageTemplate: Content
		pageUrl: GraphQLString
		parent: Content
		permissions: Permissions
		publish: PublishInfo
		site: portal_Site
		type: GraphQLString
		valid: GraphQLBoolean
		x: ExtraData
		xAsJson: GraphQLJson
	} & Extensions,
	CoreContent
>

export declare interface ContentConnection {
	totalCount: NonNull<GraphQLInt>
	edges?: ContentEdge[]
	pageInfo?: PageInfo
}

export declare interface ContentEdge {
	node: NonNull<Content>
	cursor: NonNull<GraphQLString>
}

export declare interface ContentType {
	name: GraphQLString
	displayName: GraphQLString
	description: GraphQLString
	superType: GraphQLString
	abstract: GraphQLBoolean
	final: GraphQLBoolean
	allowChildContent: GraphQLBoolean
	contentDisplayNameScript: GraphQLString
	icon: Icon
	form: FormItem[]
	formAsJson: GraphQLJson
}

export declare interface ExtraData {
	media: XData_media_ApplicationConfig
	base: XData_base_ApplicationConfig
}

export declare interface FormItem {
	formItemType: FormItemType // enum
	name: GraphQLString
	label: GraphQLString
}

export declare interface GeoPoint {
	value: GraphQLString
	latitude: GraphQLFloat
	longitude: GraphQLFloat
}

export declare interface Icon {
	mimeType: GraphQLString
	modifiedTime: GraphQLString
}

export declare type media_Image = BrandGraphQLObjectType<
	'media_Image',
	{
		data: media_Image_Data
		imageUrl: GraphQLString
		mediaUrl: GraphQLString
	},
	MediaImageContent
>

export declare interface media_Image_Data {
	media: MediaUploader
	caption: GraphQLString
	altText: GraphQLString
	artist: GraphQLString[]
	copyright: GraphQLString
	tags: GraphQLString[]
}

export declare interface MediaFocalPoint {
	x: GraphQLFloat
	y: GraphQLFloat
}

export declare interface MediaUploader {
	attachment: GraphQLString
	focalPoint: MediaFocalPoint
}

export declare interface PageInfo {
	startCursor: NonNull<GraphQLString>
	endCursor: NonNull<GraphQLString>
	hasNext: NonNull<GraphQLBoolean>
}

export declare interface Permissions {
	inheritsPermissions: GraphQLBoolean
	permissions: AccessControlEntry[]
}

export declare type portal_Site = BrandGraphQLObjectType<
	'portal_Site',
	{
		data: portal_Site_Data
	},
	PortalSiteContent
>

export declare interface portal_Site_Data {
	description: GraphQLString
}

export declare interface PrincipalKey {
	idProvider: GraphQLString
	principalId: GraphQLString
	type: PrincipalType // enum
	value: GraphQLString
}

export declare interface PublishInfo {
	from: GraphQLString
	to: GraphQLString
	first: GraphQLString
}

export declare interface XData_base_ApplicationConfig {
	gpsInfo: XData_base_gpsInfo_DataConfig
}

export declare interface XData_base_gpsInfo_DataConfig {
	geoPoint: GeoPoint
	altitude: GraphQLString
	direction: GraphQLString
}

export declare interface XData_media_ApplicationConfig {
	imageInfo: XData_media_imageInfo_DataConfig
	cameraInfo: XData_media_cameraInfo_DataConfig
}

export declare interface XData_media_cameraInfo_DataConfig {
	date: GraphQLLocalDateTime
	make: GraphQLString
	model: GraphQLString
	lens: GraphQLString
	iso: GraphQLString
	focalLength: GraphQLString
	focalLength35: GraphQLString
	exposureBias: GraphQLString
	aperture: GraphQLString[]
	shutterTime: GraphQLString
	flash: GraphQLString
	autoFlashCompensation: GraphQLString
	whiteBalance: GraphQLString
	exposureProgram: GraphQLString
	shootingMode: GraphQLString
	meteringMode: GraphQLString
	exposureMode: GraphQLString
	focusDistance: GraphQLString
	orientation: GraphQLString
}

export declare interface XData_media_imageInfo_DataConfig {
	pixelSize: GraphQLString
	imageHeight: GraphQLString
	imageWidth: GraphQLString
	contentType: GraphQLString
	description: GraphQLString
	byteSize: GraphQLString
	colorSpace: GraphQLString[]
	fileSource: GraphQLString
}
