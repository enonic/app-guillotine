package com.enonic.app.guillotine;

import java.util.Objects;

public enum ModifyUnknownFieldMode
{
	IGNORE, WARN, THROW;

	public static ModifyUnknownFieldMode from( final String value )
	{
		return ModifyUnknownFieldMode.valueOf( Objects.requireNonNullElse( value, "throw" ).toUpperCase() );
	}
}
