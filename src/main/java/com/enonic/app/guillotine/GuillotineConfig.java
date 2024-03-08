package com.enonic.app.guillotine;

public @interface GuillotineConfig
{
	String queryplayground_ui_mode() default "auto";

	boolean throw_error_on_modifying_unknown_fields() default true;
}
