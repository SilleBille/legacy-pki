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
package com.netscape.cms.policy.constraints;


import java.util.*;
import com.netscape.certsrv.policy.*;
import com.netscape.certsrv.request.PolicyResult;
import com.netscape.certsrv.request.IRequest;
import com.netscape.certsrv.base.*;
import com.netscape.certsrv.authentication.*;
import com.netscape.certsrv.common.*;
import netscape.security.x509.*;
import com.netscape.cms.policy.APolicyRule;


/**
 * This is the default revocation policy. Currently this does
 * nothing. We can later add checks like whether or not to
 * revoke expired certs ..etc here.
 *
 * @version $Revision: 14561 $, $Date: 2007-05-01 10:28:56 -0700 (Tue, 01 May 2007) $
 */
public class DefaultRevocation extends APolicyRule
    implements IRevocationPolicy, IExtendedPluginInfo {
    public DefaultRevocation() {
        NAME = "DefaultRevocation";
        DESC = "Default Revocation Policy";
    }

    /**
     * Initializes this policy rule.
     * <P>
     *
     * The entries may be of the form:
     *
     *      ra.Policy.rule.<ruleName>.implName=DefaultRevocation
     *      ra.Policy.rule.<ruleName>.enable=true
     *      ra.Policy.rule.<ruleName>.predicate= ou == engineering AND o == netscape.com
     *
     * @param config	The config store reference
     */
    public void init(ISubsystem owner, IConfigStore config)
        throws EPolicyException {
    }

    /**
     * Applies the policy on the given Request.
     * <P>
     *
     * @param req	The request on which to apply policy.
     * @return The policy result object.
     */
    public PolicyResult apply(IRequest req) {
        return PolicyResult.ACCEPTED;
    }

    /**
     * Return configured parameters for a policy rule instance.
     *
     * @return nvPairs A Vector of name/value pairs.
     */
    public Vector getInstanceParams() {
        return null;
    }

    /**
     * Return default parameters for a policy implementation.
     *
     * @return nvPairs A Vector of name/value pairs.
     */
    public Vector getDefaultParams() {
        return null;
    }

    public String[] getExtendedPluginInfo(Locale locale) {
        String[] params = {
                IExtendedPluginInfo.HELP_TOKEN + ";configuration-policyrules-defaultrevocation"
            };

        return params;
    }
}

