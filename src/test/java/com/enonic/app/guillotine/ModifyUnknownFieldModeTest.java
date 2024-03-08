package com.enonic.app.guillotine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ModifyUnknownFieldModeTest
{
	@Test
	public void testValue()
	{
		assertEquals( ModifyUnknownFieldMode.IGNORE, ModifyUnknownFieldMode.from( "ignore" ) );
		assertEquals( ModifyUnknownFieldMode.WARN, ModifyUnknownFieldMode.from( "warn" ) );
		assertEquals( ModifyUnknownFieldMode.THROW, ModifyUnknownFieldMode.from( "throw" ) );
		IllegalArgumentException exception = assertThrows( IllegalArgumentException.class, () -> ModifyUnknownFieldMode.from( "unknown" ) );
		assertEquals( IllegalArgumentException.class, exception.getClass() );
	}
}
