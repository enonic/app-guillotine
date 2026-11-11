// This is a value, do not add declare.
export enum EnumTypeName {
	ComponentType = 'ComponentType',
	DslGeoPointDistanceType = 'DslGeoPointDistanceType',
	DslOperatorType = 'DslOperatorType',
	DslSortDirectionType = 'DslSortDirectionType',
	FormItemType = 'FormItemType',
	HighlightEncoderType = 'HighlightEncoderType',
	HighlightFragmenterType = 'HighlightFragmenterType',
	HighlightOrderType = 'HighlightOrderType',
	HighlightTagsSchemaType = 'HighlightTagsSchemaType',
	MediaIntentType = 'MediaIntentType',
	PrincipalType = 'PrincipalType',
}

export declare type EnumTypeNames = keyof typeof EnumTypeName

//──────────────────────────────────────────────────────────────────────────────

// This is a value, do not add declare.
export enum ComponentType {
	page = 'page',
	layout = 'layout',
	image = 'image',
	part = 'part',
	text = 'text',
	fragment = 'fragment',
}

// This is a value, do not add declare.
export enum FormItemType {
	ItemSet = 'ItemSet',
	Layout = 'Layout',
	Input = 'Input',
	OptionSet = 'OptionSet',
}

// This is a value, do not add declare.
export enum MediaIntentType {
	download = 'download',
	inline = 'inline',
}

// This is a value, do not add declare.
export enum PrincipalType {
	user = 'user',
	group = 'group',
	role = 'role',
}
