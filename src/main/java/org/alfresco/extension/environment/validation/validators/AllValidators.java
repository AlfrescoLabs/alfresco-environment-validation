/*
 * Copyright (C) 2005-2011 Alfresco Software Limited.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.

 * As a special exception to the terms and conditions of version 2.0 of 
 * the GPL, you may redistribute this Program in connection with Free/Libre 
 * and Open Source Software ("FLOSS") applications as described in Alfresco's 
 * FLOSS exception.  You should have received a copy of the text describing 
 * the FLOSS exception, and it is also available here: 
 * http://www.alfresco.com/legal/licensing"
 */

package org.alfresco.extension.environment.validation.validators;

import java.util.Map;

import org.alfresco.extension.environment.validation.Validator;
import org.alfresco.extension.environment.validation.ValidatorCallback;


/**
 * This class provides a shortcut to all known validators.
 *
 * @author Peter Monks (pmonks@alfresco.com)
 *
 */
public class AllValidators 
    implements Validator
{
    private final static Validator[] validators = {   
                                                      new PropertiesBasedJVMValidator()
                                                      //####TODO: Reconsider the order of this list
                                                      //new JVMValidator(),
                                                      //new OSValidator(),
                                                      //new ServerHardwareValidator(),
                                                      //new NetworkValidator(),
                                                      //new ThirdPartyApplicationValidator(),
                                                      //new DBValidator()
                                                  };

    /**
     * @see org.alfresco.extension.environment.validation.Validator#validate(java.util.Map, org.alfresco.extension.environment.validation.ValidatorCallback)
     */
    public void validate(final Map parameters, final ValidatorCallback callback)
    {
        for (int i = 0; i < validators.length; i++)
        {
            validators[i].validate(parameters, callback);
        }
    }

}
