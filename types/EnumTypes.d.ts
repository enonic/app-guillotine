export declare enum GuillotineBuiltinEnumTypeName {
	ComponentType = 'ComponentType',
	FormItemType = 'FormItemType',
	MediaIntentType = 'MediaIntentType',
	Permission = 'Permission',
	PrincipalType = 'PrincipalType',
}

export declare type GuillotineBuiltinEnumTypeNames = keyof typeof GuillotineBuiltinEnumTypeName

//──────────────────────────────────────────────────────────────────────────────
// Each of the keys in GuillotineBuiltinEnumTypeName must be implemented below
// And each of the enum names below should be in GuillotineBuiltinEnumTypeName
//──────────────────────────────────────────────────────────────────────────────
export declare enum ComponentType {
	page = 'page',
	layout = 'layout',
	image = 'image',
	part = 'part',
	text = 'text',
	fragment = 'fragment',
}

export declare enum FormItemType {
	ItemSet = 'ItemSet',
	Layout = 'Layout',
	Input = 'Input',
	OptionSet = 'OptionSet',
}

export declare enum MediaIntentType {
	download = 'download',
	inline = 'inline',
}

export declare enum Permission {
	READ = 'READ',
	CREATE = 'CREATE',
	MODIFY = 'MODIFY',
	DELETE = 'DELETE',
	PUBLISH = 'PUBLISH',
	READ_PERMISSIONS = 'READ_PERMISSIONS',
	WRITE_PERMISSIONS = 'WRITE_PERMISSIONS',
}

export declare enum PrincipalType {
	user = 'user',
	group = 'group',
	role = 'role',
}
