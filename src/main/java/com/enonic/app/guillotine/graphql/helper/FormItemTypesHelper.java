package com.enonic.app.guillotine.graphql.helper;

import java.util.ArrayList;
import java.util.List;

import com.enonic.xp.form.FieldSet;
import com.enonic.xp.form.FormItem;
import com.enonic.xp.form.FormItemSet;
import com.enonic.xp.form.FormOptionSet;
import com.enonic.xp.form.Input;
import com.enonic.xp.form.Occurrences;
import com.enonic.xp.inputtype.InputTypeName;

public class FormItemTypesHelper
{
    public static List<FormItem> getFilteredFormItems( final Iterable<? extends FormItem> formItems )
    {
        List<FormItem> result = new ArrayList<>();
        for ( FormItem formItem : formItems )
        {
            if ( formItem instanceof FormItemSet && !( (FormItemSet) formItem ).iterator().hasNext() )
            {
                continue;
            }
            if ( formItem instanceof FieldSet fieldSet )
            {
                result.addAll( getFilteredFormItems( fieldSet ) );
                continue;
            }
            if ( formItem instanceof Input && ( (Input) formItem ).getInputType().equals( InputTypeName.SITE_CONFIGURATOR ) )
            {
                continue;
            }
            result.add( formItem );
        }
        return result;
    }

    public static Occurrences getOccurrences( FormItem formItem )
    {
        if ( formItem instanceof FormItemSet )
        {
            return ( (FormItemSet) formItem ).getOccurrences();
        }
        if ( formItem instanceof FormOptionSet )
        {
            return ( (FormOptionSet) formItem ).getOccurrences();
        }
        if ( formItem instanceof Input )
        {
            return ( (Input) formItem ).getOccurrences();
        }

        return Occurrences.create( 0, 1 );
    }
}
