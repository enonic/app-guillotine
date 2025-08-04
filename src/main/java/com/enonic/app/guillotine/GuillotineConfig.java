package com.enonic.app.guillotine;

public @interface GuillotineConfig
{
	String queryplayground_ui_mode() default "auto";

	String graphql_extensions_modifyUnknownField() default "throw";

	int maxQueryTokens() default 15000;
}
