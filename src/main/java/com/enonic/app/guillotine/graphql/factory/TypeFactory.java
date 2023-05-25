package com.enonic.app.guillotine.graphql.factory;

import com.enonic.app.guillotine.ServiceFacade;
import com.enonic.app.guillotine.graphql.GuillotineContext;

public class TypeFactory
{
    private final GuillotineContext context;

    private final ServiceFacade serviceFacade;

    public TypeFactory( final GuillotineContext context, final ServiceFacade serviceFacade )
    {
        this.context = context;
        this.serviceFacade = serviceFacade;
    }

    public void createTypes()
    {
        new EnumTypesFactory( context ).create();
        new AclTypesFactory( context ).create();
        new FormTypesFactory( context ).create();
        new MacroTypesFactory( context, serviceFacade ).create();
        new InputTypesFactory( context ).create();
        new GenericTypesFactory( context, serviceFacade ).create();
        new XDataTypesFactory( context, serviceFacade ).create();
        new ComponentTypesFactory( context, serviceFacade ).create();
        new ContentTypesFactory( context, serviceFacade ).create();
    }
}
