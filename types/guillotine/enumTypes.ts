// This is a value, do not add declare.
export enum EnumTypeName {
	ComponentType = 'ComponentType',
	ContentPathType = 'ContentPathType',
	DslGeoPointDistanceType = 'DslGeoPointDistanceType',
	DslOperatorType = 'DslOperatorType',
	DslSortDirectionType = 'DslSortDirectionType',
	FormItemType = 'FormItemType',
	HighlightEncoderType = 'HighlightEncoderType',
	HighlightFragmenterType = 'HighlightFragmenterType',
	HighlightOrderType = 'HighlightOrderType',
	HighlightTagsSchemaType = 'HighlightTagsSchemaType',
	MediaIntentType = 'MediaIntentType',
	Permission = 'Permission',
	PrincipalType = 'PrincipalType',
	UrlType = 'UrlType',
}

export declare type EnumTypeNames = keyof typeof EnumTypeName

//──────────────────────────────────────────────────────────────────────────────

// This is a value, do not add declare.
export enum FormItemType {
	ItemSet = 'ItemSet',
	Layout = 'Layout',
	Input = 'Input',
	OptionSet = 'OptionSet',
}

// This is a value, do not add declare.
export enum Permission {
	READ = 'READ',
	CREATE = 'CREATE',
	MODIFY = 'MODIFY',
	DELETE = 'DELETE',
	PUBLISH = 'PUBLISH',
	READ_PERMISSIONS = 'READ_PERMISSIONS',
	WRITE_PERMISSIONS = 'WRITE_PERMISSIONS',
}

// This is a value, do not add declare.
export enum PrincipalType {
	user = 'user',
	group = 'group',
	role = 'role',
}
