import type {GraphQLArgs} from '../graphQL/InputTypes'
import type {GraphQLObjectType} from '../graphQL/ObjectTypes'


export declare interface CreationCallback {
	(params: {
		addFields: (newFields: Record<string, {
			args?: GraphQLArgs
			type: GraphQLObjectType
		}>) => void
		modifyFields: (existingFields: Record<string, {
			args?: GraphQLArgs
			type: GraphQLObjectType
		}>) => void
		removeFields: (existingFields: string[]) => void
		setDescription: (newDescription: string) => void
		// TODO
		// setInterfaces: (reWrittenInterfaces: (GraphQLInterfaceType|GraphQLReferenceType)[]) => void
	}): void
}

export declare type CreationCallbacks = Record<string, CreationCallback>
