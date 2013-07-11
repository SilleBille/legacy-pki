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
package com.netscape.cms.servlet.connector;

import com.netscape.cms.servlet.common.*;
import com.netscape.cms.servlet.base.*;

import javax.servlet.*;
import javax.servlet.http.*;

import java.io.*;

import com.netscape.certsrv.common.*;
import com.netscape.certsrv.request.*;
import com.netscape.certsrv.base.*;
import com.netscape.certsrv.authority.*;
import com.netscape.certsrv.apps.CMS;
import com.netscape.certsrv.authentication.*;
import com.netscape.certsrv.authorization.*;


/**
 * TokenKeyRecoveryServlet
 *   handles "key recovery service" requests from the
 * netkey TPS
 *
 * @author Christina Fu (cfu)
 * @version $Revision$, $Date$
 */
//XXX add auditing later
public class TokenKeyRecoveryServlet extends CMSServlet {

    private final static String INFO = "TokenKeyRecoveryServlet";
    public final static String PROP_AUTHORITY = "authority";
    protected ServletConfig mConfig = null;
    protected IAuthority mAuthority = null;
    public static int ERROR = 1;
    IPrettyPrintFormat pp = CMS.getPrettyPrintFormat(":");
    protected IAuthSubsystem mAuthSubsystem = null;

    /**
     * Constructs TokenKeyRecovery servlet.
     *
     */
    public TokenKeyRecoveryServlet() {
        super();
    }

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        mConfig = config;
        String authority = config.getInitParameter(PROP_AUTHORITY);

        if (authority != null)
            mAuthority = (IAuthority)
                    CMS.getSubsystem(authority);
        
        mAuthSubsystem = (IAuthSubsystem) CMS.getSubsystem(CMS.SUBSYSTEM_AUTH);
    }

    /**
     * Returns serlvet information.
     *
     * @return name of this servlet
     */
    public String getServletInfo() { 
        return INFO; 
    }

   /**
     * Process the HTTP request.
     *
     * @param s The URL to decode
     */
     protected String URLdecode(String s) {
        if (s == null)
            return null;
        ByteArrayOutputStream out = new ByteArrayOutputStream(s.length());

        for (int i = 0; i < s.length(); i++) {
            int c = (int) s.charAt(i);

            if (c == '+') {
                out.write(' ');
            } else if (c == '%') {
                int c1 = Character.digit(s.charAt(++i), 16);
                int c2 = Character.digit(s.charAt(++i), 16);

                out.write((char) (c1 * 16 + c2));
            } else {
                out.write(c);
            }
        } // end for
        return out.toString();
     }

    /*
     * processTokenKeyRecovery
     *   handles netkey key recovery requests
     * input params are:
     *  CUID - the CUID of the old token where the keys/certs were initially for
     *  userid - the userid that belongs to both the old token and the new token
     *  drm_trans_desKey - the des key generated for the NEW token
     *                            wrapped with DRM transport key
     *  cert - the user cert corresponding to the key to be recovered
     *
     * operations: 
     *  1. unwrap des key with transport key, then url decode it
     *  2. retrieve user private key
     *  3. wrap user priv key with des key
     *  4. send the following to RA:
     *      * des key wrapped(user priv key)
     *     (note: RA should have kek-wrapped des key from TKS)
     *      * recovery blob (used for recovery)
     *
     * output params are:
     *   status=value0
     *   publicKey=value1
     *   desKey-wrapped-userPrivateKey=value2
     */
    private void processTokenKeyRecovery(HttpServletRequest req,
      HttpServletResponse resp) throws EBaseException
    {
        IRequestQueue queue = mAuthority.getRequestQueue();
        IRequest thisreq = null;
        
	//        IConfigStore sconfig = CMS.getConfigStore();
        boolean missingParam = false;
        String status = "0";

        CMS.debug("processTokenKeyRecovery begins:");

        String rCUID = req.getParameter("CUID");
        String rUserid = req.getParameter("userid");
        String rKeyid = req.getParameter("keyid");
        String rdesKeyString = req.getParameter("drm_trans_desKey");
	String rCert = req.getParameter("cert");

        if ((rCUID == null) || (rCUID.equals(""))) {
            CMS.debug("TokenKeyRecoveryServlet: processTokenKeyRecovery(): missing request parameter: CUID");
            missingParam = true;
        }

        if ((rUserid == null) || (rUserid.equals(""))) {
            CMS.debug("TokenKeyRecoveryServlet: processTokenKeyRecovery(): missing request parameter: userid");
            missingParam = true;
        }

        if ((rdesKeyString == null) ||
            (rdesKeyString.equals(""))) {
            CMS.debug("TokenKeyRecoveryServlet: processTokenKeyRecovery(): missing request parameter: DRM-transportKey-wrapped des key");
            missingParam = true;
        }

        if (((rCert == null) || (rCert.equals(""))) &&
            ((rKeyid == null) || (rKeyid.equals("")))) {
            CMS.debug("TokenKeyRecoveryServlet: processTokenKeyRecovery(): missing request parameter: cert or keyid");
            missingParam = true;
        }

        String selectedToken = null;

        if (!missingParam) {
            thisreq = queue.newRequest(IRequest.NETKEY_KEYRECOVERY_REQUEST);

            thisreq.setExtData(IRequest.REQUESTOR_TYPE, IRequest.REQUESTOR_NETKEY_RA);
            thisreq.setExtData(IRequest.NETKEY_ATTR_CUID, rCUID);
            thisreq.setExtData(IRequest.NETKEY_ATTR_USERID, rUserid);
            thisreq.setExtData(IRequest.NETKEY_ATTR_DRMTRANS_DES_KEY, rdesKeyString);
            if ((rCert != null) && (!rCert.equals(""))) {
                thisreq.setExtData(IRequest.NETKEY_ATTR_USER_CERT, rCert);
                CMS.debug("TokenKeyRecoveryServlet: processTokenKeyRecovery(): received request parameter: cert");
            }
            if ((rKeyid != null) || (!rKeyid.equals(""))) {
                thisreq.setExtData(IRequest.NETKEY_ATTR_KEYID, rKeyid); 
                CMS.debug("TokenKeyRecoveryServlet: processTokenKeyRecovery(): received request parameter: keyid");
            }

	    //XXX auto process for netkey
	    queue.processRequest( thisreq );
	    //	    IService svc = (IService) new TokenKeyRecoveryService(kra);
	    //	    svc.serviceRequest(thisreq);

            Integer result = thisreq.getExtDataInInteger(IRequest.RESULT);
            if (result != null) {
		// sighs!  tps thinks 0 is good, and drm thinks 1 is good
		if (result.intValue() == 1)
		    status ="0";
		else
		    status = result.toString();
            } else
                status = "7";

            CMS.debug("processTokenKeyRecovery finished");
        } // ! missingParam

        String value = "";

        resp.setContentType("text/html");

        String outputString = "";
        String wrappedPrivKeyString = "";
        String publicKeyString = "";
        String ivString = "";
	/* if is RECOVERY_PROTOTYPE
        String recoveryBlobString = "";

        IKeyRecord kr = (IKeyRecord) thisreq.get("keyRecord");
        byte publicKey_b[] = kr.getPublicKeyData();

        BigInteger serialNo = kr.getSerialNumber();

        String serialNumberString =
            com.netscape.cmsutil.util.Utils.SpecialEncode(serialNo.toByteArray());

        recoveryBlobString = (String)
            thisreq.get("recoveryBlob");
	*/

        if( thisreq == null ) {
            CMS.debug( "TokenKeyRecoveryServlet::processTokenKeyRecovery() - "
                     + "thisreq is null!" );
            throw new EBaseException( "thisreq is null" );
        }

        publicKeyString = thisreq.getExtDataInString("public_key");
        wrappedPrivKeyString = thisreq.getExtDataInString("wrappedUserPrivate");

        ivString = thisreq.getExtDataInString("iv_s");
        /*
          if (selectedToken == null)
          status = "4";
        */
        if (!status.equals("0")) 
            value = "status="+status;
        else {
            StringBuffer sb = new StringBuffer();
            sb.append("status=0&");
            sb.append("wrapped_priv_key=");
            sb.append(wrappedPrivKeyString);
            sb.append("&public_key=");
            sb.append(publicKeyString);
            sb.append("&iv_param=");
            sb.append(ivString);
            value = sb.toString();
            
        }
        CMS.debug("ProcessTokenKeyRecovery:outputString.encode " +value);

        try{
            resp.setContentLength(value.length());
            CMS.debug("TokenKeyRecoveryServlet:outputString.length " +value.length());
            OutputStream ooss = resp.getOutputStream();
            ooss.write(value.getBytes());
            ooss.flush();
            mRenderResult = false;
        } catch (IOException e) {
            CMS.debug("TokenKeyRecoveryServlet: " + e.toString());
        }
    }


    /* 
     *   For TokenKeyRecovery
     *
     *   input:
     *   CUID=value0
     *   trans-wrapped-desKey=value1
     *
     *   output:
     *   status=value0
     *   publicKey=value1
     *   desKey-wrapped-userPrivateKey=value2
     *   proofOfArchival=value3
     */

    public void process(CMSRequest cmsReq) throws EBaseException {
        HttpServletRequest req = cmsReq.getHttpReq();
        HttpServletResponse resp = cmsReq.getHttpResp();

        IAuthToken authToken = authenticate(cmsReq);
        AuthzToken authzToken = null;

        try {
            authzToken = authorize(mAclMethod, authToken,
                        mAuthzResourceName, "submit");
        } catch (Exception e) {
        }

        if (authzToken == null) {

            try{
                resp.setContentType("text/html");
                String value = "unauthorized=";
                CMS.debug("TokenKeyRecoveryServlet: Unauthorized");

                resp.setContentLength(value.length());
                OutputStream ooss = resp.getOutputStream();
                ooss.write(value.getBytes());
                ooss.flush();
                mRenderResult = false;
            }catch (Exception e) {
                CMS.debug("TokenKeyRecoveryServlet: " + e.toString());
            }

            cmsReq.setStatus(CMSRequest.UNAUTHORIZED);
            return;
        }

        // begin Netkey serverSideKeyGen and archival
	CMS.debug("TokenKeyRecoveryServlet: processTokenKeyRecovery would be called");
	processTokenKeyRecovery(req, resp);
	return;
        // end Netkey functions

    }

    /** XXX remember to check peer SSL cert and get RA id later
     *
     * Serves HTTP admin request.
     *
     * @param req HTTP request
     * @param resp HTTP response
     */
    public void service(HttpServletRequest req, HttpServletResponse resp)
        throws ServletException, IOException {
        String scope = req.getParameter(Constants.OP_SCOPE);
        String op = req.getParameter(Constants.OP_TYPE);

       super.service(req, resp);

 
    }

}
