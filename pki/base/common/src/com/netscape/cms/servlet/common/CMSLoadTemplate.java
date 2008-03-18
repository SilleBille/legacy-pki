// --- BEGIN COPYRIGHT BLOCK ---
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; version 2 of the License.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, write to the Free Software Foundation, Inc.,
// 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
//
// (C) 2007 Red Hat, Inc.
// All rights reserved.
// --- END COPYRIGHT BLOCK ---
package com.netscape.cms.servlet.common;


import com.netscape.certsrv.logging.*;


/**
 * handy class containing cms templates to load & fill. 
 *
 * @version $Revision: 14561 $, $Date: 2007-05-01 10:28:56 -0700 (Tue, 01 May 2007) $
 */
public class CMSLoadTemplate {
    public String mPropName;
    public String mFillerPropName;
    public String mTemplateName;
    public ICMSTemplateFiller mFiller;

    public CMSLoadTemplate() {
    }

    public CMSLoadTemplate(
        String propName, String fillerPropName, 
        String templateName, ICMSTemplateFiller filler) {
		
        mPropName = propName;
        mFillerPropName = fillerPropName;
        mTemplateName = templateName;
        mFiller = filler;
    }

    public String getPropName() {
        return mPropName;
    }

    public String getFillerPropName() {
        return mFillerPropName;
    }

    public String getTemplateName() {
        return mTemplateName;
    }

    public ICMSTemplateFiller getTemplateFiller() {
        return mFiller;
    }

}
