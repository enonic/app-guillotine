package com.enonic.app.guillotine;

public @interface GuillotineConfig
{
    String graphql_extensions_modifyUnknownField() default "throw";

    int maxQueryTokens() default 15000;

    boolean mcp_enabled() default true;

    String mcp_defaultProject() default "";

    int mcp_maxResultChars() default 20000;
}
