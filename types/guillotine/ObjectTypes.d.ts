import type {NonNull} from '../Branded'
import type {
	GuillotineFormItemType,
	GuillotinePermission,
	GuillotinePrincipalType,
} from './EnumTypes'
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


export declare enum GuillotineBuiltinObjectTypeName {
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

export declare type GuillotineBuiltinObjectTypeNames = keyof typeof GuillotineBuiltinObjectTypeName

//──────────────────────────────────────────────────────────────────────────────

export declare interface GuillotineAccessControlEntry {
	principal: GuillotinePrincipalKey
	allow: GuillotinePermission[] // enum
	deny: GuillotinePermission[] // enum
}

export declare interface GuillotineAttachment {
	name: GraphQLString
	label: GraphQLString
	size: GraphQLInt
	mimeType: GraphQLString
	attachmentUrl: GraphQLString
}

export declare type GuillotineContent<
	Extensions extends Record<string, unknown> = Record<string, unknown>
> = {
	_id: NonNull<GraphQLID>
	_name: NonNull<GraphQLString>
	_path: NonNull<GraphQLString>
	_references: GuillotineContent[]
	_score: GraphQLFloat
	attachments: GuillotineAttachment[]
	children: GuillotineContent[]
	childrenConnection: GuillotineContentConnection
	components: GuillotineContent[]
	contentType: GuillotineContentType
	createdTime: GraphQLDateTime
	creator: GuillotinePrincipalKey
	dataAsJson: GraphQLJson
	displayName: GraphQLString
	hasChildren: GraphQLBoolean
	language: GraphQLString
	modifiedTime: GraphQLDateTime
	modifier: GuillotinePrincipalKey
	owner: GuillotinePrincipalKey
	pageAsJson: GraphQLJson
	pageTemplate: GuillotineContent
	pageUrl: GraphQLString
	parent: GuillotineContent
	permissions: GuillotinePermissions
	publish: GuillotinePublishInfo
	site: Guillotineportal_Site
	type: GraphQLString
	valid: GraphQLBoolean
	x: GuillotineExtraData
	xAsJson: GraphQLJson
} & Extensions

export declare interface GuillotineContentConnection {
	totalCount: NonNull<GraphQLInt>
	edges?: GuillotineContentEdge[]
	pageInfo?: GuillotinePageInfo
}

export declare interface GuillotineContentEdge {
	node: NonNull<GuillotineContent>
	cursor: NonNull<GraphQLString>
}

export declare interface GuillotineContentType {
	name: GraphQLString
	displayName: GraphQLString
	description: GraphQLString
	superType: GraphQLString
	abstract: GraphQLBoolean
	final: GraphQLBoolean
	allowChildContent: GraphQLBoolean
	contentDisplayNameScript: GraphQLString
	icon: GuillotineIcon
	form: GuillotineFormItem[]
	formAsJson: GraphQLJson
}

export declare interface GuillotineExtraData {
	media: GuillotineXData_media_ApplicationConfig
	base: GuillotineXData_base_ApplicationConfig
}

export declare interface GuillotineFormItem {
	formItemType: GuillotineFormItemType // enum
	name: GraphQLString
	label: GraphQLString
}

export declare interface GuillotineGeoPoint {
	value: GraphQLString
	latitude: GraphQLFloat
	longitude: GraphQLFloat
}

export declare interface GuillotineIcon {
	mimeType: GraphQLString
	modifiedTime: GraphQLString
}

export declare interface Guillotinemedia_Image extends GuillotineContent {
	data: Guillotinemedia_Image_Data
	imageUrl: GraphQLString
	mediaUrl: GraphQLString
}

export declare interface Guillotinemedia_Image_Data {
	media: GuillotineMediaUploader
	caption: GraphQLString
	altText: GraphQLString
	artist: GraphQLString[]
	copyright: GraphQLString
	tags: GraphQLString[]
}

export declare interface GuillotineMediaFocalPoint {
	x: GraphQLFloat
	y: GraphQLFloat
}

export declare interface GuillotineMediaUploader {
	attachment: GraphQLString
	focalPoint: GuillotineMediaFocalPoint
}

export declare interface GuillotinePageInfo {
	startCursor: NonNull<GraphQLString>
	endCursor: NonNull<GraphQLString>
	hasNext: NonNull<GraphQLBoolean>
}

export declare interface GuillotinePermissions {
	inheritsPermissions: GraphQLBoolean
	permissions: GuillotineAccessControlEntry[]
}

export declare interface Guillotineportal_Site extends GuillotineContent {
	data: Guillotineportal_Site_Data
}

export declare interface Guillotineportal_Site_Data {
	description: GraphQLString
}

export declare interface GuillotinePrincipalKey {
	idProvider: GraphQLString
	principalId: GraphQLString
	type: GuillotinePrincipalType // enum
	value: GraphQLString
}

export declare interface GuillotinePublishInfo {
	from: GraphQLString
	to: GraphQLString
	first: GraphQLString
}

export declare interface GuillotineXData_base_ApplicationConfig {
	gpsInfo: GuillotineXData_base_gpsInfo_DataConfig
}

export declare interface GuillotineXData_base_gpsInfo_DataConfig {
	geoPoint: GuillotineGeoPoint
	altitude: GraphQLString
	direction: GraphQLString
}

export declare interface GuillotineXData_media_ApplicationConfig {
	imageInfo: GuillotineXData_media_imageInfo_DataConfig
	cameraInfo: GuillotineXData_media_cameraInfo_DataConfig
}

export declare interface GuillotineXData_media_cameraInfo_DataConfig {
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

export declare interface GuillotineXData_media_imageInfo_DataConfig {
	pixelSize: GraphQLString
	imageHeight: GraphQLString
	imageWidth: GraphQLString
	contentType: GraphQLString
	description: GraphQLString
	byteSize: GraphQLString
	colorSpace: GraphQLString[]
	fileSource: GraphQLString
}
