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

package org.alfresco.extension.util;


/**
 * Utility class for containing two things that aren't like each other.
 * 
 * @author Peter Monks (peter.monks@alfresco.com)
 * @version $Id$
 */
public final class ComparablePair
    implements Comparable
{
    /**
     * The first member of the triple.
     */
    private final Comparable first;
    
    /**
     * The second member of the triple.
     */
    private final Comparable second;
    
    /**
     * Make a new one.
     * 
     * @param first  The first member.
     * @param second The second member.
     */
    public ComparablePair(final Comparable first, final Comparable second)
    {
        this.first  = first;
        this.second = second;
    }
    
    /**
     * Get the first member of the tuple.
     * @return The first member.
     */
    public Object getFirst()
    {
        return first;
    }
    
    /**
     * Get the second member of the tuple.
     * @return The second member.
     */
    public Object getSecond()
    {
        return second;
    }
    
    /**
     * Override of equals.
     * @param other The thing to compare to.
     * @return equality.
     */
    public boolean equals(final Object other)
    {
        if (this == other)
        {
            return true;
        }
        
        if (!(other instanceof ComparablePair))
        {
            return false;
        }
        
        ComparablePair o = (ComparablePair)other;
        return (first.equals(o.getFirst()) &&
                second.equals(o.getSecond()));
    }
    
    /**
     * Override of hashCode.
     */
    public int hashCode()
    {
        return ((first  == null ? 0 : first.hashCode()) +
                (second == null ? 0 : second.hashCode()));
    }
    
    

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(final Object aThat)
    {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;
        
        if (aThat == null)
        {
            throw new NullPointerException("Attempted to call compareTo with null object");
        }
        
        if (!(aThat instanceof ComparablePair))
        {
            throw new IllegalStateException("Attempted to compare class org.alfresco.extension.util.ComparablePair with class " + aThat.getClass().toString());
        }
        
        ComparablePair that = (ComparablePair)aThat;

        //this optimization is usually worthwhile, and can
        //always be added
        if (this == that) return EQUAL;

        //objects, including type-safe enums, follow this form
        //note that null objects will throw an exception here
        int comparison = this.first.compareTo(that.first);
        if (comparison != EQUAL) return comparison;

        comparison = this.second.compareTo(that.second);
        if ( comparison != EQUAL ) return comparison;

        //all comparisons have yielded equality
        //verify that compareTo is consistent with equals (optional)
        assert this.equals(that) : "compareTo inconsistent with equals.";

        return(EQUAL);
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString()
    {
        return "(" + first + ", " + second + ")";
    }
}
