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
package com.netscape.admin.certsrv.config;

import java.util.*;
import javax.swing.*;
import com.netscape.admin.certsrv.*;
import com.netscape.admin.certsrv.connection.*;
import com.netscape.certsrv.common.*;
import com.netscape.management.client.util.Debug;


/**
 * Rule instance Data model - represents the instance
 * table information
 *
 * @author Jack Pan-Chen
 * @version $Revision: 14593 $, $Date: 2007-05-01 16:35:45 -0700 (Tue, 01 May 2007) $
 */
public class RuleRuleDataModel extends CMSRuleDataModel
{

    /*==========================================================
     * constructors
     *==========================================================*/
    public RuleRuleDataModel() {
        super();
    }

	protected String[] getColumns() {
		Debug.println("PolicyRuleDataModel.getColumns()");
		String x[] = {RULE_RULE, STATUS};
		return x;
	}

    public void processData(Object data) {
        Vector v = new Vector();
        NameValuePairs obj = (NameValuePairs) data;

        if (obj.getValue(RULE_STAT).equalsIgnoreCase("enabled")) {
            v.addElement(new JLabel(obj.getValue(RULE_NAME),
                    CMSAdminUtil.getImage(CMSAdminResources.IMAGE_RULE),
                    JLabel.LEFT));
            v.addElement(mResource.getString("RULERULE_LABEL_ENABLED_LABEL"));       
        } else {
            v.addElement(new JLabel(obj.getValue(RULE_NAME),
                    CMSAdminUtil.getImage(CMSAdminResources.IMAGE_RULE_DISABLE),
                    JLabel.LEFT));
            v.addElement(mResource.getString("RULERULE_LABEL_DISABLED_LABEL"));
        }
        addRow(v, data);
        mRules.addElement(obj.getValue(RULE_NAME));
    }


}
