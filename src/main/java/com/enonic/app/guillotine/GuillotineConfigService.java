package com.enonic.app.guillotine;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

@Component(immediate = true, service = GuillotineConfigService.class, configurationPid = "com.enonic.app.guillotine")
public class GuillotineConfigService
{

	private QueryPlaygroundUIMode queryPlaygroundUIMode;

	private ModifyUnknownFieldMode modifyUnknownFieldMode;

	@Activate
	@Modified
	public void activate( final GuillotineConfig config )
	{
		this.queryPlaygroundUIMode = QueryPlaygroundUIMode.from( config.queryplayground_ui_mode() );
		this.modifyUnknownFieldMode = ModifyUnknownFieldMode.from( config.graphql_extensions_modifyUnknownField() );
	}

	public QueryPlaygroundUIMode getQueryPlaygroundUIMode()
	{
		return queryPlaygroundUIMode;
	}

	public ModifyUnknownFieldMode getModifyUnknownFieldMode()
	{
		return modifyUnknownFieldMode;
	}
}
