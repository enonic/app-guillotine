import type {
	ComponentType,
	FormItemType,
	MediaIntentType,
	Permission,
	PrincipalType,
} from './EnumTypes'


export declare enum GuillotineBuiltinObjectTypeName {
	AccessControlEntry = 'AccessControlEntry',
	Content = 'Content',
	ContentType = 'ContentType',
	FormItem = 'FormItem',
	Icon = 'Icon',
	media_Image = 'media_Image',
	PrincipalKey = 'PrincipalKey'
}

export declare type GuillotineBuiltinObjectTypeNames = keyof typeof GuillotineBuiltinObjectTypeName

//──────────────────────────────────────────────────────────────────────────────
// Each of the keys in GuillotineBuiltinObjectTypeName must be implemented below
// And each of the type names below should be in GuillotineBuiltinObjectTypeName
//──────────────────────────────────────────────────────────────────────────────

interface AccessControlEntry {
	principal: PrincipalKey
	allow: Permission[]
	deny: Permission[]
}

interface Attachment {
	name: string
	label: string
	size: number // Int
	mimeType: string
	attachmentUrl: string
}

interface Component {
	type: ComponentType, // ComponentType!
	path: string, // String!
	part?: PartComponentData
	page?: PageComponentData
	layout?: LayoutComponentData
	image?: ImageComponentData
	text?: TextComponentData
	fragment?: FragmentComponentData
}

interface Content {
	_id: string // ID!
	_name: string // String!
	_path: string // String!
	_references: Content[]
	_score: number // Float
	attachments: Attachment[]
	children: Content[]
	childrenConnection: ContentConnection
	components: Component[]
	contentType: ContentType
	createdTime: string // DateTime
	creator: PrincipalKey
	dataAsJson: JSON
	displayName: String
	hasChildren: Boolean
	language: String
	modifiedTime: string // DateTime
	modifier: PrincipalKey
	owner: PrincipalKey
	pageAsJson: JSON
	pageTemplate: Content
	pageUrl: String
	parent: Content
	permissions: Permissions
	publish: PublishInfo
	site: portal_Site
	type: String
	valid: Boolean
	x: ExtraData
	xAsJson: JSON
}

interface ContentConnection {
	totalCount: number // Int! // TODO Use branded?
	edges?: ContentEdge[]
	pageInfo?: PageInfo
}

interface ContentEdge {
	node: Content // Content!
	cursor: string // String!
}

interface ContentType {
	name: string
	displayName: string
	description: string
	superType: string
	abstract: boolean
	final: boolean
	allowChildContent: boolean
	contentDisplayNameScript: string
	icon: Icon
	form: FormItem[]
	formAsJson: JSON
}

interface ExtraData {
	media: XData_media_ApplicationConfig
	base: XData_base_ApplicationConfig
}

interface FormItem {
	formItemType: FormItemType
	name: string
	label: string
}

interface FragmentComponentData {
	id: string // ID! // TODO Use branded?
	fragment?: Content
}

interface GeoPoint {
	value: string
	latitude: number // Float // TODO Use branded?
	longitude: number // Float // TODO Use branded?
}

interface Icon {
	mimeType: string
	modifiedTime: string
}

interface Image {
	image: Content
	ref: string
	style: ImageStyle
}

interface ImageComponentData {
	id: string // ID!
	caption?: string
	image?: media_Image
}

interface ImageStyle {
	name: string
	aspectRatio: string
	filter: string
}

interface LayoutComponentData<
	LayoutComponentDataConfig extends Record<string, unknown> = Record<string, unknown>
> {
	descriptor: string // String!
	configAsJson?: JSON
	config?: LayoutComponentDataConfig
}

interface Link {
	ref: string
	uri: string
	media: Media
	content: Content
}

interface Macro {
	ref: string
	name: string
	descriptor: string
	config: MacroConfig
}

interface Macro_system_disable_DataConfig {
	body: string
}

interface Macro_system_embed_DataConfig {
	body: string
}

interface MacroConfig {
	disable: Macro_system_disable_DataConfig
	embed: Macro_system_embed_DataConfig
	[fieldName: string]: unknown
}

interface Media {
	content: Content
	intent: MediaIntentType
}

interface media_Image extends Content {
	_id: string // ID!
	_name: string // String!
	_path: string // String!
	_references: Content[]
	_score: number // Float
	attachments: Attachment[]
	children: Content[]
	childrenConnection: ContentConnection
	components: Component[]
	contentType: ContentType
	createdTime: string // DateTime
	creator: PrincipalKey
	data: media_Image_Data
	dataAsJson: JSON
	displayName: string
	hasChildren: boolean
	imageUrl: string
	language: string
	mediaUrl: string
	modifiedTime: string // DateTime
	modifier: PrincipalKey
	owner: PrincipalKey
	pageAsJson: JSON
	pageTemplate: Content
	pageUrl: string
	parent: Content
	permissions: Permissions
	publish: PublishInfo
	site: portal_Site
	type: string
	valid: boolean
	x: ExtraData
	xAsJson: JSON
}

interface media_Image_Data {
	media: MediaUploader
	caption: string
	altText: string
	artist: string[]
	copyright: string
	tags: string[]
}

// type media_Image = Content<{
// 	altText?: string
// 	artist?: string|string[]
// 	caption?: string
// 	copyright?: string
// 	media: {
// 		attachment: string
// 		focalPoint: {
// 			x: number
// 			y: number
// 		}
// 	}
// 	tags?: string|string[]
// }, 'media:image'>

interface MediaFocalPoint {
	x: number // Float
	y: number // Float
}

interface MediaUploader {
	attachment: string
	focalPoint: MediaFocalPoint
}

interface PageComponentData<
	PageComponentDataConfig extends Record<string, unknown> = Record<string, unknown>
> {
	descriptor: string
	customized: boolean
	template: Content
	configAsJson: JSON
	config: PageComponentDataConfig
}

interface PartComponentData<
	PartComponentDataConfig extends Record<string, unknown> = Record<string, unknown>
> {
	descriptor: string // String!
	configAsJson: JSON
	config: PartComponentDataConfig
}

interface PageInfo {
	startCursor: string // String!
	endCursor: string // String!
	hasNext: boolean // Boolean!
}

interface Permissions {
	inheritsPermissions: boolean
	permissions: AccessControlEntry[]
}

interface portal_Site extends Content {
	_id: string // ID!
	_name: string // String!
	_path: string // String!
	_references: Content[]
	_score: number // Float
	attachments: Attachment[]
	children: Content[]
	childrenConnection: ContentConnection
	components: Component[]
	contentType: ContentType
	createdTime: string // DateTime
	creator: PrincipalKey
	data: portal_Site_Data
	dataAsJson: JSON
	displayName: string
	hasChildren: boolean
	language: string
	modifiedTime: string // DateTime
	modifier: PrincipalKey
	owner: PrincipalKey
	pageAsJson: JSON
	pageTemplate: Content
	pageUrl: string
	parent: Content
	permissions: Permissions
	publish: PublishInfo
	site: portal_Site
	type: string
	valid: boolean
	x: ExtraData
	xAsJson: JSON
}

interface portal_Site_Data {
	description: string
}

interface PrincipalKey {
	idProvider: string
	principalId: string
	type: PrincipalType
	value: string
}

interface PublishInfo {
	from: string
	to: string
	first: string
}

interface RichText {
	raw: string
	processedHtml: string
	macrosAsJson: JSON
	macros: Macro[]
	images: Image[]
	links: Link[]
}

interface TextComponentData {
	value: RichText // RichText!
}

interface XData_base_ApplicationConfig {
	gpsInfo: XData_base_gpsInfo_DataConfig
}

interface XData_base_gpsInfo_DataConfig {
	geoPoint: GeoPoint
	altitude: string
	direction: string
}

interface XData_media_ApplicationConfig {
	imageInfo: XData_media_imageInfo_DataConfig
	cameraInfo: XData_media_cameraInfo_DataConfig
}

interface XData_media_cameraInfo_DataConfig {
	date: string // LocalDateTime // TODO Use branded?
	make: string
	model: string
	lens: string
	iso: string
	focalLength: string
	focalLength35: string
	exposureBias: string
	aperture: string[]
	shutterTime: string
	flash: string
	autoFlashCompensation: string
	whiteBalance: string
	exposureProgram: string
	shootingMode: string
	meteringMode: string
	exposureMode: string
	focusDistance: string
	orientation: string
}

interface XData_media_imageInfo_DataConfig {
	pixelSize: string
	imageHeight: string
	imageWidth: string
	contentType: string
	description: string
	byteSize: string
	colorSpace: string[]
	fileSource: string
}
