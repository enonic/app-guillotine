import type {GraphQLInterfaceType} from '../graphQL/InterfaceTypes'
import type {GraphQLInterfaceTypeReference} from '../graphQL/ReferenceTypes'
import type {Field} from './Field'


export declare interface CreationCallback {
	(params: {
		addFields: (newFields: Record<string, Field>) => void
		modifyFields: (existingFields: Record<string, Field>) => void
		removeFields: (existingFields: string[]) => void
		setDescription: (newDescription: string) => void
		setInterfaces: (reWrittenInterfaces: GraphQLInterfaceTypeReference<GraphQLInterfaceType>[]) => void
	}): void
}

export declare type CreationCallbacks = Record<string, CreationCallback>
