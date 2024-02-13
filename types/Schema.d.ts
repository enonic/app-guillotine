// import type {Content} from '@enonic-types/core'
import type {GraphQLBranded} from './Branded'
import type {JSON} from './Utils'
import type {GuillotineBuiltinEnumTypeNames} from './EnumTypes'
import type {
	Content,
	ContentType,
	FormItem,
	GuillotineBuiltinObjectTypeName,
	GuillotineBuiltinObjectTypeNames,
	Icon,
	media_Image,
	PrincipalKey,
} from './ObjectTypes'

//──────────────────────────────────────────────────────────────────────────────
// Guillotine GraphQL Object types
//──────────────────────────────────────────────────────────────────────────────
export declare type GraphQLContent = GraphQLBranded<Content, GuillotineBuiltinObjectTypeName.Content>

export declare type GraphQLContentType = GraphQLBranded<ContentType, GuillotineBuiltinObjectTypeName.ContentType>

export declare type GraphQLFormItem = GraphQLBranded<FormItem, GuillotineBuiltinObjectTypeName.FormItem>

export declare type GraphQLIcon = GraphQLBranded<Icon, GuillotineBuiltinObjectTypeName.Icon>

export declare type GraphQLmedia_Image = GraphQLBranded<media_Image, GuillotineBuiltinObjectTypeName.media_Image>

export declare type GraphQLPrincipalKey = GraphQLBranded<PrincipalKey, GuillotineBuiltinObjectTypeName.PrincipalKey>


//──────────────────────────────────────────────────────────────────────────────
// "Union" types
//──────────────────────────────────────────────────────────────────────────────
export type GuillotineBuiltinTypeNames =
	| GuillotineBuiltinEnumTypeNames
	| GuillotineBuiltinObjectTypeNames
