export declare enum GuillotineBuiltinEnumTypeName {
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

export declare enum GuillotineFormItemType {
	ItemSet = 'ItemSet',
	Layout = 'Layout',
	Input = 'Input',
	OptionSet = 'OptionSet',
}

export declare enum GuillotinePermission {
	READ = 'READ',
	CREATE = 'CREATE',
	MODIFY = 'MODIFY',
	DELETE = 'DELETE',
	PUBLISH = 'PUBLISH',
	READ_PERMISSIONS = 'READ_PERMISSIONS',
	WRITE_PERMISSIONS = 'WRITE_PERMISSIONS',
}

export declare enum GuillotinePrincipalType {
	user = 'user',
	group = 'group',
	role = 'role',
}
