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
package com.netscape.cms.servlet.cert;


import com.netscape.cms.servlet.common.*;
import com.netscape.cms.servlet.base.*;
import java.io.*;
import java.util.*;
import java.math.*;
import javax.servlet.*;
import java.security.cert.*;
import javax.servlet.http.*;
import netscape.ldap.*;
import netscape.security.x509.*;
import netscape.security.pkcs.*;
import com.netscape.certsrv.base.*;
import com.netscape.certsrv.policy.*;
import com.netscape.certsrv.request.IRequest;
import com.netscape.certsrv.dbs.*;
import com.netscape.certsrv.dbs.certdb.*;
import com.netscape.certsrv.ldap.*;
import com.netscape.certsrv.authority.ICertAuthority;
import com.netscape.certsrv.dbs.*;
import com.netscape.certsrv.dbs.crldb.*;
import com.netscape.certsrv.logging.*;
import com.netscape.certsrv.apps.*;
import com.netscape.certsrv.ra.*;
import com.netscape.cms.authentication.*;
import com.netscape.certsrv.authentication.*;
import com.netscape.certsrv.authorization.*;
import com.netscape.cms.servlet.*;


/**
 * XXX Servlet to get the enrollment status, enable or disable.
 *
 * @version $Revision: 14561 $, $Date: 2007-05-01 10:28:56 -0700 (Tue, 01 May 2007) $
 */
public class GetEnableStatus extends CMSServlet {
    private final static String TPL_FILE = "userEnroll.template";
    private String mFormPath = null;

    public GetEnableStatus() {
        super();
    }

    /**
	 * initialize the servlet.
     * @param sc servlet configuration, read from the web.xml file
     */
    public void init(ServletConfig sc) throws ServletException {
        super.init(sc);
        // coming from agent
        mFormPath = "/" + mAuthority.getId() + "/" + TPL_FILE;

        mTemplates.remove(CMSRequest.SUCCESS);
    }

    protected CMSRequest newCMSRequest() {
        return new CMSRequest();
    }

    /**
     * Process the HTTP request. 
     * <ul>
     * <li>http.param
     * </ul>
     *
     * @param cmsReq the object holding the request and response information
     */
    protected void process(CMSRequest cmsReq)
        throws EBaseException {
        HttpServletRequest httpReq = cmsReq.getHttpReq();
        HttpServletResponse httpResp = cmsReq.getHttpResp();

        IAuthToken authToken = authenticate(cmsReq);
        AuthzToken authzToken = null;

        try {
            authzToken = authorize(mAclMethod, authToken,
                        mAuthzResourceName, "read");
        } catch (Exception e) {
            // do nothing for now
        }

        if (authzToken == null) {
            cmsReq.setStatus(CMSRequest.UNAUTHORIZED);
            return;
        }

        String reqHost = httpReq.getRemoteHost();

        // Construct an ArgBlock
        IArgBlock args = cmsReq.getHttpParams();

        if (!(mAuthority instanceof IRegistrationAuthority)) {
            log(ILogger.LL_FAILURE, CMS.getLogMessage("CMSGW_CA_FROM_RA_NOT_IMP"));
            cmsReq.setError(new ECMSGWException(
              CMS.getUserMessage("CMS_GW_NOT_YET_IMPLEMENTED")));
            cmsReq.setStatus(CMSRequest.ERROR);
            return;
        }

        CMSTemplate form = null;
        Locale[] locale = new Locale[1];

        try {
            form = getTemplate(mFormPath, httpReq, locale);
        } catch (IOException e) {
            log(ILogger.LL_FAILURE, 
                CMS.getLogMessage("CMSGW_ERR_GET_TEMPLATE",
                    mFormPath, e.toString()));
            cmsReq.setError(new ECMSGWException(
              CMS.getUserMessage("CMS_GW_DISPLAY_TEMPLATE_ERROR")));
            cmsReq.setStatus(CMSRequest.ERROR);
            return;
        }

        IArgBlock header = CMS.createArgBlock();
        IArgBlock fixed = CMS.createArgBlock();

        CMSTemplateParams argSet = new CMSTemplateParams(header, fixed);

        IConfigStore configStore = CMS.getConfigStore();
        String val = configStore.getString("hashDirEnrollment.name");
        IAuthSubsystem authSS = (IAuthSubsystem) CMS.getSubsystem(CMS.SUBSYSTEM_AUTH);
        IAuthManager authMgr = authSS.get(val);
        HashAuthentication mgr = (HashAuthentication) authMgr;
        long timeout = mgr.DEFAULT_TIMEOUT / 1000;

        header.addStringValue("timeout", "" + timeout);
        header.addStringValue("reqHost", reqHost);

        for (Enumeration hosts = mgr.getHosts(); hosts.hasMoreElements();) {
            IArgBlock rarg = CMS.createArgBlock();

            rarg.addStringValue("hosts", (String) hosts.nextElement());
            argSet.addRepeatRecord(rarg);
        }

        try {
            ServletOutputStream out = httpResp.getOutputStream();

            httpResp.setContentType("text/html");
            form.renderOutput(out, argSet);
            cmsReq.setStatus(CMSRequest.SUCCESS);
        } catch (IOException e) {
            log(ILogger.LL_FAILURE, 
                CMS.getLogMessage("CMSGW_ERR_STREAM_TEMPLATE", e.toString()));
            cmsReq.setError(new ECMSGWException(
              CMS.getUserMessage("CMS_GW_DISPLAY_TEMPLATE_ERROR")));
            cmsReq.setStatus(CMSRequest.ERROR);
        }
        cmsReq.setStatus(CMSRequest.SUCCESS);
        return;
    }

}
