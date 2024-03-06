import type {GraphQLArgs} from '../graphQL/InputTypes'
import type {GraphQLObjectType} from '../graphQL/ObjectTypes'
import type {GraphQLInterfaceTypeReference} from '../graphQL/ReferenceTypes'
import type {Fields} from './Field'


export declare interface CreationCallback {
	(params: {
		addFields: (newFields: Fields) => void
		modifyFields: (existingFields: Fields) => void
		removeFields: (existingFields: string[]) => void
		setDescription: (newDescription: string) => void
		setInterfaces: (reWrittenInterfaces: GraphQLInterfaceTypeReference[]) => void
	}): void
}

export declare type CreationCallbacks = Record<string, CreationCallback>
