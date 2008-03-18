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
package com.netscape.cms.profile.def;


import java.io.*;
import java.security.cert.*;
import java.util.*;
import com.netscape.certsrv.base.*;
import com.netscape.certsrv.profile.*;
import com.netscape.certsrv.request.*;
import com.netscape.certsrv.property.*;
import com.netscape.certsrv.apps.*;
import com.netscape.certsrv.common.*;

import netscape.security.x509.*;
import netscape.security.extensions.*;
import netscape.security.util.*;
import com.netscape.cms.profile.common.*;


/**
 * This class implements an enrollment default policy 
 * that populates Subject Info Access extension.
 *
 * @version $Revision: 14561 $, $Date: 2007-05-01 10:28:56 -0700 (Tue, 01 May 2007) $
 */
public class SubjectInfoAccessExtDefault extends EnrollExtDefault {

    public static final String CONFIG_CRITICAL = "subjInfoAccessCritical";
    public static final String CONFIG_NUM_ADS = "subjInfoAccessNumADs";
    public static final String CONFIG_AD_ENABLE = "subjInfoAccessADEnable_";
    public static final String CONFIG_AD_METHOD = "subjInfoAccessADMethod_";
    public static final String CONFIG_AD_LOCATIONTYPE = "subjInfoAccessADLocationType_";
    public static final String CONFIG_AD_LOCATION = "subjInfoAccessADLocation_";

    public static final String VAL_CRITICAL = "subjInfoAccessCritical";
    public static final String VAL_GENERAL_NAMES = "subjInfoAccessGeneralNames";

    private static final String AD_METHOD = "Method";
    private static final String AD_LOCATION_TYPE = "Location Type";
    private static final String AD_LOCATION = "Location";
    private static final String AD_ENABLE = "Enable";

    private static final int DEF_NUM_AD = 5;

    public SubjectInfoAccessExtDefault() {
        super();
        addValueName(VAL_CRITICAL);
        addValueName(VAL_GENERAL_NAMES);

        // register configuration names bases on num ads
        addConfigName(CONFIG_CRITICAL);
        int num = getNumAds();

        for (int i = 0; i < num; i++) {
            addConfigName(CONFIG_AD_METHOD + i);
            addConfigName(CONFIG_AD_LOCATIONTYPE + i);
            addConfigName(CONFIG_AD_LOCATION + i);
            addConfigName(CONFIG_AD_ENABLE + i);
        }
    }

    protected int getNumAds() {
        int num = DEF_NUM_AD;
        String numAds = getConfig(CONFIG_NUM_ADS);

        if (numAds != null) {
            try {
                num = Integer.parseInt(numAds);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return num;
    }
 
    public void init(IProfile profile, IConfigStore config)
        throws EProfileException {
        super.init(profile, config);

    }

    public IDescriptor getConfigDescriptor(Locale locale, String name) { 
        if (name.equals(CONFIG_CRITICAL)) {
            return new Descriptor(IDescriptor.BOOLEAN, null, 
                    "false",
                    CMS.getUserMessage(locale, "CMS_PROFILE_CRITICAL"));
        } else if (name.startsWith(CONFIG_AD_METHOD)) {
            return new Descriptor(IDescriptor.STRING, null, 
                    null,
                    CMS.getUserMessage(locale, "CMS_PROFILE_AD_METHOD"));
        } else if (name.startsWith(CONFIG_AD_LOCATIONTYPE)) {
            return new Descriptor(IDescriptor.CHOICE, "RFC822Name,DNSName,DirectoryName,EDIPartyName,URIName,IPAddress,OIDName", 
                    "URIName",
                    CMS.getUserMessage(locale, "CMS_PROFILE_AD_LOCATIONTYPE"));
        } else if (name.startsWith(CONFIG_AD_LOCATION)) {
            return new Descriptor(IDescriptor.STRING, null, 
                    null,
                    CMS.getUserMessage(locale, "CMS_PROFILE_AD_LOCATION"));
        } else if (name.startsWith(CONFIG_AD_ENABLE)) {
            return new Descriptor(IDescriptor.BOOLEAN, null, 
                    "false",
                    CMS.getUserMessage(locale, "CMS_PROFILE_AD_ENABLE"));
        } 
        return null;
    }

    public IDescriptor getValueDescriptor(Locale locale, String name) {
        if (name.equals(VAL_CRITICAL)) {
            return new Descriptor(IDescriptor.BOOLEAN, null, 
                    "false",
                    CMS.getUserMessage(locale, "CMS_PROFILE_CRITICAL"));
        } else if (name.equals(VAL_GENERAL_NAMES)) {
            return new Descriptor(IDescriptor.STRING_LIST, null, 
                    null,
                    CMS.getUserMessage(locale, "CMS_PROFILE_GENERAL_NAMES"));
        } else {
            return null;
        }
    }

    public void setValue(String name, Locale locale,
        X509CertInfo info, String value)
        throws EPropertyException {
        try {
            SubjectInfoAccessExtension ext = null;

            if (name == null) { 
                throw new EPropertyException(CMS.getUserMessage( 
                            locale, "CMS_INVALID_PROPERTY", name));
            }

           
            SubjectInfoAccessExtension a = new SubjectInfoAccessExtension(false);
            ObjectIdentifier oid = a.getExtensionId();

            ext = (SubjectInfoAccessExtension)
                        getExtension(oid.toString(), info); 

            if(ext == null)  {
                populate(null,info);
            }
 
            if (name.equals(VAL_CRITICAL)) {

                ext = (SubjectInfoAccessExtension)
                        getExtension(oid.toString(), info);
                boolean val = Boolean.valueOf(value).booleanValue();

                if(ext == null)
                {
                    return;
                }
                ext.setCritical(val); 
            } else if (name.equals(VAL_GENERAL_NAMES)) { 

                ext = (SubjectInfoAccessExtension)
                        getExtension(oid.toString(), info);

                if(ext == null)
                {
                    return;
                }
                boolean critical = ext.isCritical();

                Vector v = parseRecords(value);
                int size = v.size();

                ext = new SubjectInfoAccessExtension(critical);
                String method = null;
                String locationType = null;
                String location = null;
                String enable = null;

                for (int i = 0; i < size; i++) {
                    NameValuePairs nvps = (NameValuePairs) v.elementAt(i);
                    Enumeration names = nvps.getNames();

                    while (names.hasMoreElements()) {
                        String name1 = (String) names.nextElement();

                        if (name1.equals(AD_METHOD)) {
                            method = nvps.getValue(name1);
                        } else if (name1.equals(AD_LOCATION_TYPE)) {
                            locationType = nvps.getValue(name1);
                        } else if (name1.equals(AD_LOCATION)) {
                            location = nvps.getValue(name1);
                        } else if (name1.equals(AD_ENABLE)) {
                            enable = nvps.getValue(name1);
                        }
                    }

                    if (enable != null && enable.equals("true")) {
                        GeneralName gn = null;

                        if (locationType != null || location != null) {
                            GeneralNameInterface interface1 = parseGeneralName(locationType + ":" + location);
                            if (interface1 == null)
                                throw new EPropertyException(CMS.getUserMessage(
                                  locale, "CMS_INVALID_PROPERTY", locationType));
                            gn = new GeneralName(interface1);
                        }
     
                        if (method != null) {
                            try {
                                ext.addAccessDescription(new ObjectIdentifier(method), gn); 
                            } catch (NumberFormatException ee) {
                                CMS.debug("SubjectInfoAccessExtDefault: "+ee.toString());
                                throw new EPropertyException(CMS.getUserMessage(
                                  locale, "CMS_PROFILE_DEF_SIA_OID", method));
                            }
                        }
                    }
                }
            } else {
                throw new EPropertyException(CMS.getUserMessage(
                            locale, "CMS_INVALID_PROPERTY", name));
            }

            replaceExtension(ext.getExtensionId().toString(), ext, info);
        } catch (IOException e) {
            CMS.debug("SubjectInfoAccessExtDefault: " + e.toString());
            throw new EPropertyException(CMS.getUserMessage(
                        locale, "CMS_INVALID_PROPERTY", name));
        } catch (EProfileException e) {
            CMS.debug("SubjectInfoAccessExtDefault: " + e.toString());
            throw new EPropertyException(CMS.getUserMessage(
                        locale, "CMS_INVALID_PROPERTY", name));
        }
    }

    public String getValue(String name, Locale locale,
        X509CertInfo info)
        throws EPropertyException {
        SubjectInfoAccessExtension ext = null;

        if (name == null) { 
            throw new EPropertyException(CMS.getUserMessage(
                        locale, "CMS_INVALID_PROPERTY", name));
        }

        SubjectInfoAccessExtension a = new SubjectInfoAccessExtension(false);
            ObjectIdentifier oid = a.getExtensionId();

        ext = (SubjectInfoAccessExtension)
                    getExtension(oid.toString(), info);

        if(ext == null)
        {
            try {
                populate(null,info);

            } catch (EProfileException e) {
                CMS.debug("SubjectInfoAccessExtDefault: getValue " + e.toString());
                 throw new EPropertyException(CMS.getUserMessage(
                      locale, "CMS_INVALID_PROPERTY", name));
            }

        }
        if (name.equals(VAL_CRITICAL)) {

            ext = (SubjectInfoAccessExtension)
                    getExtension(oid.toString(), info);

            if (ext == null) {
                return null;
            }
            if (ext.isCritical()) {
                return "true";
            } else {
                return "false";
            }
        } else if (name.equals(VAL_GENERAL_NAMES)) { 

            ext = (SubjectInfoAccessExtension)
                    getExtension(oid.toString(), info);

            if (ext == null)
                return "";

            int num = DEF_NUM_AD;
	
            CMS.debug("SubjectInfoAccess num=" + num);
            Vector recs = new Vector();

            for (int i = 0; i < num; i++) { 
                NameValuePairs np = new NameValuePairs();
                AccessDescription des = null;

                if (i < ext.numberOfAccessDescription()) {
                    des = ext.getAccessDescription(i);
                }
                if (des == null) {
                    np.add(AD_METHOD, "");
                    np.add(AD_LOCATION_TYPE, "");
                    np.add(AD_LOCATION, "");
                    np.add(AD_ENABLE, "false");
                } else {
                    ObjectIdentifier methodOid = des.getMethod();
                    GeneralName gn = des.getLocation(); 

                    np.add(AD_METHOD, methodOid.toString());
                    np.add(AD_LOCATION_TYPE, getGeneralNameType(gn));
                    np.add(AD_LOCATION, getGeneralNameValue(gn));
                    np.add(AD_ENABLE, "true");
                }
                recs.addElement(np);
            }

            return buildRecords(recs);
        } else {
            throw new EPropertyException(CMS.getUserMessage(
                        locale, "CMS_INVALID_PROPERTY", name));
        }
    }

    public String getText(Locale locale) {
        StringBuffer ads = new StringBuffer();
        String numAds = getConfig(CONFIG_NUM_ADS);
        int num = getNumAds();

        for (int i = 0; i < num; i++) {
            ads.append("Record #");
            ads.append(i);
            ads.append("{");
            ads.append(AD_METHOD + ":");
            ads.append(getConfig(CONFIG_AD_METHOD + i));
            ads.append(",");
            ads.append(AD_LOCATION_TYPE + ":");
            ads.append(getConfig(CONFIG_AD_LOCATIONTYPE + i));
            ads.append(",");
            ads.append(AD_LOCATION + ":");
            ads.append(getConfig(CONFIG_AD_LOCATION + i));
            ads.append(",");
            ads.append(AD_ENABLE + ":");
            ads.append(getConfig(CONFIG_AD_ENABLE + i));
            ads.append("}");
        }
        return CMS.getUserMessage(locale, "CMS_PROFILE_DEF_SIA_TEXT", 
                getConfig(CONFIG_CRITICAL), ads.toString());
    }

    /**
     * Populates the request with this policy default.
     */
    public void populate(IRequest request, X509CertInfo info)
        throws EProfileException {
        SubjectInfoAccessExtension ext = createExtension();

        addExtension(ext.getExtensionId().toString(), ext, info);
    }

    public SubjectInfoAccessExtension createExtension() {
        SubjectInfoAccessExtension ext = null; 
        int num = getNumAds();

        try {
            boolean critical = getConfigBoolean(CONFIG_CRITICAL);

            ext = new SubjectInfoAccessExtension(critical);
            for (int i = 0; i < num; i++) {
                String enable = getConfig(CONFIG_AD_ENABLE + i);
                if (enable != null && enable.equals("true")) {
                    CMS.debug("SubjectInfoAccess: createExtension i=" + i);
                    String method = getConfig(CONFIG_AD_METHOD + i);
                    String locationType = getConfig(CONFIG_AD_LOCATIONTYPE + i);
                    if (locationType == null || locationType.length() == 0)
                        locationType = "URIName";
                    String location = getConfig(CONFIG_AD_LOCATION + i);

                    if (location == null || location.equals("")) {
                        if (method.equals("1.3.6.1.5.5.7.48.1")) {
                            String hostname = CMS.getEENonSSLHost();
                            String port = CMS.getEENonSSLPort();
                            if (hostname != null && port != null)
                                location = "http://"+hostname+":"+port+"/ocsp";
                        }
                    }

                    String s = locationType + ":" + location;
                    GeneralNameInterface gn = parseGeneralName(s);
                    if (gn != null) {
                      ext.addAccessDescription(new ObjectIdentifier(method), 
                        new GeneralName(gn)); 
                    }
                }
            }
        } catch (Exception e) {
            CMS.debug("SubjectInfoAccessExtDefault: createExtension " + 
                e.toString());
        }

        return ext;
    }
}
