// This is a value, do not add declare.
export enum GuillotineBuiltinEnumTypeName {
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

export declare type GuillotineBuiltinEnumTypeNames = keyof typeof GuillotineBuiltinEnumTypeName

//──────────────────────────────────────────────────────────────────────────────

// This is a value, do not add declare.
export enum GuillotineFormItemType {
	ItemSet = 'ItemSet',
	Layout = 'Layout',
	Input = 'Input',
	OptionSet = 'OptionSet',
}

// This is a value, do not add declare.
export enum GuillotinePermission {
	READ = 'READ',
	CREATE = 'CREATE',
	MODIFY = 'MODIFY',
	DELETE = 'DELETE',
	PUBLISH = 'PUBLISH',
	READ_PERMISSIONS = 'READ_PERMISSIONS',
	WRITE_PERMISSIONS = 'WRITE_PERMISSIONS',
}

// This is a value, do not add declare.
export enum GuillotinePrincipalType {
	user = 'user',
	group = 'group',
	role = 'role',
}
