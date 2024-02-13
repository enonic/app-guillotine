import type {GraphQLArgs} from '../graphQL/InputTypes'
import type {GraphQLObjectType} from '../graphQL/ObjectTypes'
import type {GraphQLInterfaceTypeReference} from '../graphQL/ReferenceTypes'
import type {Field} from './Field'


export declare interface CreationCallback {
	(params: {
		addFields: (newFields: Record<string, Field>) => void
		modifyFields: (existingFields: Record<string, Field>) => void
		removeFields: (existingFields: string[]) => void
		setDescription: (newDescription: string) => void
		setInterfaces: (reWrittenInterfaces: GraphQLInterfaceTypeReference[]) => void
	}): void
}

export declare type CreationCallbacks = Record<string, CreationCallback>
