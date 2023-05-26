package com.enonic.app.guillotine.graphql.transformer;

import java.util.LinkedHashMap;
import java.util.Map;

import com.enonic.xp.script.ScriptValue;

public class SchemaExtensions
{
    private final Map<String, ScriptValue> types;

    private final Map<String, ScriptValue> inputTypes;

    private final Map<String, ScriptValue> enums;

    private final Map<String, ScriptValue> unions;

    private final Map<String, ScriptValue> interfaces;

    private final Map<String, Map<String, ScriptValue>> resolvers;

    private final Map<String, ScriptValue> typeResolvers;

    private final Map<String, ScriptValue> creationCallbacks;

    private SchemaExtensions( final Builder builder )
    {
        this.types = builder.types;
        this.inputTypes = builder.inputTypes;
        this.enums = builder.enums;
        this.unions = builder.unions;
        this.interfaces = builder.interfaces;
        this.resolvers = builder.resolvers;
        this.typeResolvers = builder.typeResolvers;
        this.creationCallbacks = builder.creationCallbacks;
    }

    public Map<String, ScriptValue> getTypes()
    {
        return types;
    }

    public Map<String, ScriptValue> getInputTypes()
    {
        return inputTypes;
    }

    public Map<String, ScriptValue> getEnums()
    {
        return enums;
    }

    public Map<String, ScriptValue> getUnions()
    {
        return unions;
    }

    public Map<String, ScriptValue> getInterfaces()
    {
        return interfaces;
    }

    public Map<String, Map<String, ScriptValue>> getResolvers()
    {
        return resolvers;
    }

    public Map<String, ScriptValue> getTypeResolvers()
    {
        return typeResolvers;
    }

    public Map<String, ScriptValue> getCreationCallbacks()
    {
        return creationCallbacks;
    }

    public static Builder create()
    {
        return new Builder();
    }

    public static class Builder
    {
        private final Map<String, ScriptValue> types = new LinkedHashMap<>();

        private final Map<String, ScriptValue> inputTypes = new LinkedHashMap<>();

        private final Map<String, ScriptValue> enums = new LinkedHashMap<>();

        private final Map<String, ScriptValue> unions = new LinkedHashMap<>();

        private final Map<String, ScriptValue> interfaces = new LinkedHashMap<>();

        private final Map<String, Map<String, ScriptValue>> resolvers = new LinkedHashMap<>();

        private final Map<String, ScriptValue> typeResolvers = new LinkedHashMap<>();

        private final Map<String, ScriptValue> creationCallbacks = new LinkedHashMap<>();

        private Builder()
        {
            // do nothing
        }

        public Builder addType( String typeName, ScriptValue typeDef )
        {
            validate( typeName, typeDef );
            this.types.put( typeName, typeDef );
            return this;
        }

        public Builder addInputType( String typeName, ScriptValue typeDef )
        {
            validate( typeName, typeDef );
            this.inputTypes.put( typeName, typeDef );
            return this;
        }

        public Builder addEnum( String typeName, ScriptValue typeDef )
        {
            this.enums.put( typeName, typeDef );
            return this;
        }

        public Builder addUnion( String typeName, ScriptValue typeDef )
        {
            this.unions.put( typeName, typeDef );
            return this;
        }

        public Builder addInterface( String typeName, ScriptValue typeDef )
        {
            this.interfaces.put( typeName, typeDef );
            return this;
        }

        public Builder addResolver( String typeName, String fieldName, ScriptValue resolverDef )
        {
            this.resolvers.computeIfAbsent( typeName, k -> new LinkedHashMap<>() );
            this.resolvers.get( typeName ).put( fieldName, resolverDef );
            return this;
        }

        public Builder addTypeResolver( String typeName, ScriptValue resolverDef )
        {
            this.typeResolvers.put( typeName, resolverDef );
            return this;
        }

        public Builder addCreationCallback( String typeName, ScriptValue creationCallbackDef )
        {
            this.creationCallbacks.put( typeName, creationCallbackDef );
            return this;
        }

        private static void validate( final String typeName, final ScriptValue typeDef )
        {
            if ( typeDef == null || !typeDef.isObject() )
            {
                throw new IllegalArgumentException( String.format( "'%s' must be object", typeName ) );
            }
        }

        public SchemaExtensions build()
        {
            return new SchemaExtensions( this );
        }
    }
}
