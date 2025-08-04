package com.enonic.app.guillotine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class GuillotineConfigServiceTest
{
	@Test
	void testMaxQuerySize()
	{
		GuillotineConfig config = mock( GuillotineConfig.class, invocationOnMock -> invocationOnMock.getMethod().getDefaultValue() );

		GuillotineConfigService instance = new GuillotineConfigService();
		instance.activate( config );
		assertEquals( 15000, instance.getMaxQueryTokens() );

		when( config.maxQueryTokens() ).thenReturn( 100000 );
		instance.activate( config );
		assertEquals( 100000, instance.getMaxQueryTokens() );
	}

}
