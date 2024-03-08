package com.enonic.app.guillotine;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;

@Component(immediate = true, service = GuillotineConfigService.class, configurationPid = "com.enonic.app.guillotine")
public class GuillotineConfigService
{

	private QueryPlaygroundUIMode queryPlaygroundUIMode;

	private boolean throwErrorOnModifyingUnknownFields;

	@Activate
	@Modified
	public void activate( final GuillotineConfig config )
	{
		this.queryPlaygroundUIMode = QueryPlaygroundUIMode.from( config.queryplayground_ui_mode() );
		this.throwErrorOnModifyingUnknownFields = config.throw_error_on_modifying_unknown_fields();
	}

	public QueryPlaygroundUIMode getQueryPlaygroundUIMode()
	{
		return queryPlaygroundUIMode;
	}

	public boolean isThrowErrorOnModifyingUnknownFields()
	{
		return throwErrorOnModifyingUnknownFields;
	}
}
