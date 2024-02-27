# Guillotine types

## Use

```ts
import type {GraphQL, Extensions} from '@enonic-types/guillotine';


import {ObjectTypeName} from '@enonic-types/guillotine';


const MY_OBJECT_TYPE_NAME = 'MyObjectType';
const MY_FIELD_NAME = 'myField';


export const extensions = (graphQL: GraphQL): Extensions => {
  return {
    types: {
      [MY_OBJECT_TYPE_NAME]: {
        description: 'Description for my object type',
        fields: {
          myString: {
            type: graphQL.nonNull(graphQL.GraphQLString),
          }
        }
      }
    },
    creationCallbacks: {
      [ObjectTypeName.Content]: (params) => {
        params.addFields({
          [MY_FIELD_NAME]: {
            type: graphQL.reference(MY_OBJECT_TYPE_NAME)
          }
        });
      }
    },
    resolvers: {
      [ObjectTypeName.Content]: {
        [MY_FIELD_NAME]: () => {
          const {
            // args,
            // localContext,
            source: content
          } = env;
          if (content.type === 'portal:fragment') {
            return null;
          }
          return {
            myString: 'Hello, World!'
          }
        },
      }
    }
  }
};

```
