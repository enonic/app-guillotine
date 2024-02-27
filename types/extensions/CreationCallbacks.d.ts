import type {GraphQLArgs} from '../graphQL/InputTypes'
import type {GraphQLObjectType} from '../graphQL/ObjectTypes'
import type {Field} from './Field'


export declare interface CreationCallback {
	(params: {
		addFields: (newFields: Record<string, Field>) => void
		modifyFields: (existingFields: Record<string, Field>) => void
		removeFields: (existingFields: string[]) => void
		setDescription: (newDescription: string) => void
		// TODO
		// setInterfaces: (reWrittenInterfaces: (GraphQLInterfaceType|GraphQLReferenceType)[]) => void
	}): void
}

export declare type CreationCallbacks = Record<string, CreationCallback>
