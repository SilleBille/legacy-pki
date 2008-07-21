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
package com.netscape.cms.servlet.csadmin;


import org.apache.velocity.Template;
import org.apache.velocity.servlet.VelocityServlet;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.context.Context;
import javax.servlet.*;
import javax.servlet.http.*;
import netscape.ldap.*;
import com.netscape.certsrv.apps.*;
import com.netscape.certsrv.property.*;
import com.netscape.certsrv.dbs.*;
import com.netscape.certsrv.util.*;
import com.netscape.certsrv.base.*;
import com.netscape.certsrv.authorization.*;
import com.netscape.certsrv.authentication.*;
import com.netscape.certsrv.usrgrp.*;
import com.netscape.certsrv.ca.*;
import java.io.*;
import java.util.*;
import com.netscape.cmsutil.ldap.*;

import com.netscape.cms.servlet.wizard.*;

public class DatabasePanel extends WizardPanelBase {

    private static final String HOST = "localhost";
    private static final String PORT = "389";
    private static final String BASEDN = "o=netscapeCertificateServer";
    private static final String BINDDN = "cn=Directory Manager";
    private static final String DATABASE = "csRoot";
    private static final String MASTER_AGREEMENT = "masteragreement-";
    private static final String CLONE_AGREEMENT = "cloneagreement-";

    private WizardServlet mServlet = null;

    public DatabasePanel() {}

    /**
     * Initializes this panel.
     */
    public void init(ServletConfig config, int panelno) 
        throws ServletException {
        setPanelNo(panelno);
        setName("Internal Database");
    }

    public void init(WizardServlet servlet, ServletConfig config, int panelno, String id)
        throws ServletException {
        setPanelNo(panelno);
        setName("Internal Database");
        setId(id);
        mServlet = servlet;
    }

    public void cleanUp() throws IOException {
        IConfigStore cs = CMS.getConfigStore();
        cs.putBoolean("preop.Database.done", false);
    }

    public boolean isPanelDone() {
        IConfigStore cs = CMS.getConfigStore();
        try {
            boolean s = cs.getBoolean("preop.Database.done",
                    false);

            if (s != true) {
                return false;
            } else {
                return true;
            }
        } catch (EBaseException e) {}

        return false;
    }

    public PropertySet getUsage() {
        PropertySet set = new PropertySet();
        Descriptor hostDesc = new Descriptor(IDescriptor.STRING, null, null,
                "Host name");

        set.add("hostname", hostDesc);
      
        Descriptor portDesc = new Descriptor(IDescriptor.INTEGER, null, null,
                "Port");

        set.add("portStr", portDesc);

        Descriptor basednDesc = new Descriptor(IDescriptor.STRING, null, null,
                "Base DN");

        set.add("basedn", basednDesc);
 
        Descriptor binddnDesc = new Descriptor(IDescriptor.STRING, null, null,
                "Bind DN");

        set.add("binddn", binddnDesc);

        Descriptor bindpwdDesc = new Descriptor(IDescriptor.PASSWORD, null, null,
                "Bind Password"); 

        set.add("bindpwd", bindpwdDesc);

        Descriptor databaseDesc = new Descriptor(IDescriptor.STRING, null, null,
                "Database");

        set.add("database", databaseDesc);

        return set;
    }

    /**
     * Display the panel.
     */
    public void display(HttpServletRequest request,
            HttpServletResponse response,
            Context context) {
        CMS.debug("DatabasePanel: display()");
        context.put("title", "Internal Database");
        context.put("firsttime", "false");
        IConfigStore cs = CMS.getConfigStore();
        String hostname = null;
        String portStr = null;
        String basedn = null;
        String binddn = null;
        String bindpwd = "";
        String database = null;
        String errorString = "";
        String secure = "false";
        try {
            String s = cs.getString("preop.database.removeData");
        } catch (Exception e) {
            context.put("firsttime", "true");
        }

        String select = "";
        try {
            select = cs.getString("preop.subsystem.select", "");
        } catch (Exception e) {
        }

        if (isPanelDone()) {
            try {
                hostname = cs.getString("internaldb.ldapconn.host", "");
                portStr = cs.getString("internaldb.ldapconn.port", "");
                basedn = cs.getString("internaldb.basedn", "");
                binddn = cs.getString("internaldb.ldapauth.bindDN", "");
                database = cs.getString("internaldb.database", "");
            	secure =  cs.getString("internaldb.ldapconn.secureConn", "");
                errorString = cs.getString("preop.database.errorString", "");
            } catch (Exception e) {
                CMS.debug("DatabasePanel display: " + e.toString());
            }
        } else if (select.equals("clone")) {
            hostname = HOST;
            portStr = PORT;
            try {
                basedn = cs.getString("internaldb.basedn", "");
            } catch (Exception e) {
                CMS.debug( "DatabasePanel::display() - "
                         + "Exception="+e.toString() );
                return;
            }
            binddn = BINDDN;
            database = basedn.substring(basedn.lastIndexOf('=')+1);
            CMS.debug("Clone: database=" + database);
        } else {
            hostname = HOST;
            portStr = PORT;
            String instanceId = "";
            String machineName = "";

            try {
                instanceId = cs.getString("instanceId", "");
                machineName = cs.getString("machineName", "");
            } catch (Exception e) {
                CMS.debug("DatabasePanel display: " + e.toString());
            }
            String suffix = "dc=" + machineName + "-" + instanceId;

            boolean multipleEnable = false;
            try {
                multipleEnable = cs.getBoolean(
                  "internaldb.multipleSuffix.enable", false);
            } catch (Exception e) {
            }
      
        
            if (multipleEnable)
                basedn = "ou=" + instanceId + "," + suffix;
            else
                basedn = suffix;
            binddn = BINDDN;
            database = machineName + "-" + instanceId;
        }

        context.put("clone", select);
        context.put("hostname", hostname);
        context.put("portStr", portStr);
        context.put("basedn", basedn);
        context.put("binddn", binddn);
        context.put("bindpwd", bindpwd);
        context.put("database", database);
		context.put("secureConn", (secure.equals("true")? "on":"off"));
        context.put("panel", "admin/console/config/databasepanel.vm");
        context.put("errorString", errorString);
    }

    public void initParams(HttpServletRequest request, Context context)
                   throws IOException
    {
        IConfigStore config = CMS.getConfigStore();
        String select = "";
        try {
            select = config.getString("preop.subsystem.select", "");
        } catch (Exception e) {
        }
        context.put("clone", select);
        context.put("hostname", request.getParameter("host"));
        context.put("portStr", request.getParameter("port"));
        context.put("basedn", request.getParameter("basedn"));
        context.put("binddn", request.getParameter("binddn"));
        context.put("bindpwd", request.getParameter("__bindpwd"));
        context.put("database", request.getParameter("database"));
    }

    /**
     * Checks if the given parameters are valid.
     */
    public void validate(HttpServletRequest request,
            HttpServletResponse response,
            Context context) throws IOException {

        IConfigStore cs = CMS.getConfigStore();
        context.put("firsttime", "false");
        try {
            String s = cs.getString("preop.database.removeData");
        } catch (Exception e) {
            context.put("firsttime", "true");
        }

        String hostname = HttpInput.getHostname(request, "host");
        context.put("hostname", hostname);

        String portStr = HttpInput.getPortNumber(request, "port");
        context.put("portStr", portStr);

        String basedn = HttpInput.getDN(request, "basedn");
        context.put("basedn", basedn);

        String binddn = HttpInput.getDN(request, "binddn");
        context.put("binddn", binddn);

        String database = HttpInput.getLdapDatabase(request, "database");
        context.put("database", database);

        String bindpwd = HttpInput.getPassword(request, "__bindpwd");
        context.put("bindpwd", bindpwd);

        String secure = HttpInput.getCheckbox(request, "secureConn");
        context.put("secureConn", secure);

        String select = "";
        try {
            select = cs.getString("preop.subsystem.select", "");
        } catch (Exception e) {
        }

        if (select.equals("clone")) {
            String masterhost = "";
            String masterport = "";
            String masterbasedn = "";
            try {
                masterhost = cs.getString("preop.internaldb.master.hostname", "");
                masterport = cs.getString("preop.internaldb.master.port", "");
                masterbasedn = cs.getString("preop.internaldb.master.basedn", "");
            } catch (Exception e) {
            }

            //get the real host name
            String realhostname = "";
            if (hostname.equals("localhost")) {
                try {
                    realhostname = cs.getString("machineName", "");
                } catch (Exception ee) {
                }
            }
            if (masterhost.equals(realhostname) && masterport.equals(portStr))
                throw new IOException("Master and clone must not share the same internal database");

            if (!masterbasedn.equals(basedn))
                throw new IOException("Master and clone should have the same base DN");
        }

        if (hostname == null || hostname.length() == 0) {
            cs.putString("preop.database.errorString", "Host is empty string");
            throw new IOException("Host is empty string");
        }

        if (portStr != null && portStr.length() > 0) {
            int port = -1;

            try {
                port = Integer.parseInt(portStr);
            } catch (Exception e) {
                cs.putString("preop.database.errorString", "Port is invalid");
                throw new IOException("Port is invalid");
            }
        } else {
            cs.putString("preop.database.errorString", "Port is empty string");
            throw new IOException("Port is empty string");
        }

        if (basedn == null || basedn.length() == 0) {
            cs.putString("preop.database.errorString", "Base DN is empty string");
            throw new IOException("Base DN is empty string");
        }

        if (binddn == null || binddn.length() == 0) {
            cs.putString("preop.database.errorString", "Bind DN is empty string");
            throw new IOException("Bind DN is empty string");
        }

        if (database == null || database.length() == 0) {
            cs.putString("preop.database.errorString",
                    "Database is empty string");
            throw new IOException("Database is empty string");
        }

        if (bindpwd == null || bindpwd.length() == 0) {
            cs.putString("preop.database.errorString",
                    "Bind password is empty string");
            throw new IOException("Bind password is empty string");
        }

        context.put("errorString", "");
        cs.putString("preop.database.errorString", "");
    }

    private LDAPConnection getLocalLDAPConn(Context context, String secure)
                throws IOException
    {
        IConfigStore cs = CMS.getConfigStore();

        String host = "";
        String port = "";
        String pwd = "";
        String binddn = "";
        String security = "";

        try {
            host = cs.getString("internaldb.ldapconn.host");
            port = cs.getString("internaldb.ldapconn.port");
            binddn = cs.getString("internaldb.ldapauth.bindDN");
            pwd = (String) context.get("bindpwd");    
            security = cs.getString("internaldb.ldapconn.secureConn");
        } catch (Exception e) {
            CMS.debug("DatabasePanel populateDB: " + e.toString());
            throw new IOException(
                    "Failed to retrieve LDAP information from CS.cfg.");
        }

        int p = -1;

        try {
            p = Integer.parseInt(port);
        } catch (Exception e) {
            CMS.debug("DatabasePanel populateDB: " + e.toString());
            throw new IOException("Port is not valid");
        }

        LDAPConnection conn = null;
        if (security.equals("true")) {
          CMS.debug("DatabasePanel populateDB: creating secure (SSL) connection for internal ldap");
          conn = new LDAPConnection(CMS.getLdapJssSSLSocketFactory());
	} else {
          CMS.debug("DatabasePanel populateDB: creating non-secure (non-SSL) connection for internal ldap");
          conn = new LDAPConnection();
	}

        CMS.debug("DatabasePanel connecting to " + host + ":" + p);
        try {
            conn.connect(host, p, binddn, pwd);
        } catch (LDAPException e) {
            CMS.debug("DatabasePanel populateDB: " + e.toString());
            throw new IOException("Failed to connect to the internal database.");
        }

      return conn;
    }

    private void populateDB(HttpServletRequest request, Context context, String secure) 
        throws IOException {
        IConfigStore cs = CMS.getConfigStore();

        String baseDN = "";

        try {
            baseDN = cs.getString("internaldb.basedn");
        } catch (Exception e) {
            CMS.debug("DatabasePanel populateDB: " + e.toString());
            throw new IOException(
                    "Failed to retrieve LDAP information from CS.cfg.");
        }

        LDAPConnection conn = getLocalLDAPConn(context, secure);

        try {
            LDAPEntry entry = conn.read(baseDN);
            if (entry != null) {
                CMS.debug("DatabasePanel update: This base DN has already been used.");
                throw new IOException("This base DN ("+baseDN+") has already been used.");
            }
        } catch (LDAPException e) {
            CMS.debug("DatabasePanel update: Exception="+e.toString());
        }

        // create database
        String dn = "";
        String database = "";
        try {
            database = cs.getString("internaldb.database", "");
            LDAPAttributeSet attrs = new LDAPAttributeSet();
            String oc[] = { "top", "extensibleObject", "nsBackendInstance"};
            attrs.add(new LDAPAttribute("objectClass", oc));
            attrs.add(new LDAPAttribute("cn", database));
            attrs.add(new LDAPAttribute("nsslapd-suffix", baseDN));
            dn = "cn=" + database + ",cn=ldbm database, cn=plugins, cn=config";
            LDAPEntry entry = new LDAPEntry(dn, attrs);
            conn.add(entry);
        } catch (LDAPException e) {
            if (e.getLDAPResultCode() == 68) {
                CMS.debug("This database has already been used.");
                throw new IOException("This database ("+dn+") has already been used.");
            }
        } catch (Exception e) {
            CMS.debug("Warning: database creation error - " + e.toString());
            throw new IOException("Failed to create the database.");
        }

        try {
            LDAPAttributeSet attrs = new LDAPAttributeSet();
            String oc2[] = { "top", "extensibleObject", "nsMappingTree"};
            attrs.add(new LDAPAttribute("objectClass", oc2));
            attrs.add(new LDAPAttribute("cn", baseDN));
            attrs.add(new LDAPAttribute("nsslapd-backend", database));
            attrs.add(new LDAPAttribute("nsslapd-state", "Backend"));
            dn = "cn=\"" + baseDN + "\",cn=mapping tree, cn=config";
            LDAPEntry entry = new LDAPEntry(dn, attrs);
            conn.add(entry);
        } catch (LDAPException e) {
            if (e.getLDAPResultCode() == 68) {
                CMS.debug("This database has already been used.");
                throw new IOException("This database ("+dn+") has already been used.");
            }
        } catch (Exception e) {
            CMS.debug("Warning: database creation error - " + e.toString());
            throw new IOException("Failed to create the database.");
        }

        try {
            // create base dn
            String dns3[] = LDAPDN.explodeDN(baseDN, false);
            StringTokenizer st = new StringTokenizer(dns3[0], "=");
            String n = st.nextToken();
            String v = st.nextToken();
            LDAPAttributeSet attrs = new LDAPAttributeSet();
            String oc3[] = { "top", "domain"};
            if (n.equals("o")) {
              oc3[1] = "organization";
            } else if (n.equals("ou")) {
              oc3[1] = "organizationalUnit";
            } 
            attrs.add(new LDAPAttribute("objectClass", oc3));
            attrs.add(new LDAPAttribute(n, v));
            LDAPEntry entry = new LDAPEntry(baseDN, attrs);
            conn.add(entry);
        } catch (LDAPException e) {
            if (e.getLDAPResultCode() == 68) {
                CMS.debug("DatabasePanel: the baseDN has already been used.");
                throw new IOException("This baseDN: "+baseDN+" has been used.");
            }
        } catch (Exception e) {
            CMS.debug("Warning: suffix creation error - " + e.toString());
            throw new IOException("Failed to create the base DN: "+baseDN);
        }

        // check if base DN is already being used
        CMS.debug("DatabasePanel checking existing cn=ClonedSubsystems, ou=groups," + baseDN);
        try {
            LDAPEntry entry = conn.read("cn=ClonedSubsystems, ou=groups," + baseDN);

            if (entry != null) {
                String remove = HttpInput.getID(request, "removeData");
                if (remove == null)
                    throw new IOException(
                        "Another instance is using this base DN " + baseDN);
                else {
                    CMS.debug("Will remove the existing data from the base DN");
                    String[] entries={"cn=Directory Administrators,"+baseDN,
                      "ou=Groups,"+baseDN, "ou=People,"+baseDN,
                      "ou=Special Users,"+baseDN,
                      "cn=Accounting Managers, ou=groups,"+baseDN,
                      "cn=HR Managers, ou=groups,"+baseDN,
                      "cn=QA Managers, ou=groups,"+baseDN,
                      "cn=PD Managers, ou=groups,"+baseDN};
                    String filter = "objectclass=*";
                    LDAPSearchConstraints cons = null;
                    String[] attrs = null;
                    LDAPSearchResults res = conn.search(baseDN, 1, filter,
                      attrs, true, cons);
                    deleteEntries(res, conn, baseDN, entries);
                }
            }
        } catch (LDAPException e) {}

        // check to see if the base dn exists
        CMS.debug("DatabasePanel checking existing " + baseDN);
        boolean foundBaseDN = false;

        try {
            LDAPEntry entry = conn.read(baseDN);

            if (entry != null) {
                foundBaseDN = true; 
            }
        } catch (LDAPException e) {}
        boolean createBaseDN = true;

        boolean testing = false;
        try {
            testing = cs.getBoolean("internaldb.multipleSuffix.enable", false);
        } catch (Exception e) {}

        if (!foundBaseDN) {
            if (!testing) {
                context.put("errorString", "Base DN was not found. Please make sure to create the suffix in the internal database.");
                throw new IOException("Base DN not found");
            }

            if (createBaseDN) {
                // only auto create if it is an ou entry
                String dns1[] = LDAPDN.explodeDN(baseDN, false);

                if (dns1 == null) {
                    throw new IOException("Invalid base DN");
                }
                if (!dns1[0].startsWith("ou")) {
                    throw new IOException(
                            "Failed to find base DN, and failed to create non ou entry.");
                }
                String dns2[] = LDAPDN.explodeDN(baseDN, true);
                // support only one level creation - create new entry
                // right under the suffix
                LDAPAttributeSet attrs = new LDAPAttributeSet();
                String oc[] = { "top", "organizationalUnit"};

                attrs.add(new LDAPAttribute("objectClass", oc));
                attrs.add(new LDAPAttribute("ou", dns2[0]));
                LDAPEntry entry = new LDAPEntry(baseDN, attrs);

                try {
                    conn.add(entry);
                    foundBaseDN = true; 
                    CMS.debug("DatabasePanel added " + baseDN);
                } catch (LDAPException e) {
                    throw new IOException("Failed to create " + baseDN);
                }
            }
        }
        if (!foundBaseDN) {
            throw new IOException("Failed to find base DN");
        }

        String select = "";
        try {
            select = cs.getString("preop.subsystem.select", "");
        } catch (Exception e) {
        }

        importLDIFS("preop.internaldb.ldif", conn);
        if (select.equals("clone")) {
          // if this is clone, add index before replication
          importLDIFS("preop.internaldb.index_ldif", conn);
        } else {
          // data will be replicated from the master to the clone
          // so clone does not need the data
          importLDIFS("preop.internaldb.data_ldif", conn);
          importLDIFS("preop.internaldb.index_ldif", conn);
        }

        try {
            conn.disconnect();
        } catch (LDAPException e) {}
    }

    private void importLDIFS(String param, LDAPConnection conn) throws IOException {
        IConfigStore cs = CMS.getConfigStore();
        String v = null;

        CMS.debug("DatabasePanel populateDB param=" + param);
        try {
            v = cs.getString(param);
        } catch (EBaseException e) {  
            CMS.debug("DatabasePanel populateDB: " + e.toString());
            throw new IOException("Cant find ldif files.");
        }
 
        StringTokenizer tokenizer = new StringTokenizer(v, ",");
        String baseDN = null;
        String database = null;

        try {
            baseDN = cs.getString("internaldb.basedn");
        } catch (EBaseException e) {
            throw new IOException("internaldb.basedn is missing.");
        }

        try {
            database = cs.getString("internaldb.database");
            CMS.debug("DatabasePanel update: database=" + database);
        } catch (EBaseException e) {
            CMS.debug(
                    "DatabasePanel update: Failed to get database name. Exception: "
                            + e.toString());
            database = "userRoot";
        }

        String instancePath = null;

        try {
            instancePath = cs.getString("instanceRoot");
        } catch (EBaseException e) {
            throw new IOException("instanceRoot is missing");
        }

        String instanceId = null;

        try {
            instanceId = cs.getString("instanceId"); 
        } catch (EBaseException e) {
            throw new IOException("instanceId is missing");
        }


        String configDir = instancePath + File.separator + "conf"; 

        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            int index = token.lastIndexOf("/");
            String name = token;

            if (index != -1) {
                name = token.substring(index + 1);
            }

            CMS.debug("DatabasePanel importLDIFS: ldif file = " + token);
            String filename = configDir + File.separator + name;

            CMS.debug("DatabasePanel importLDIFS: ldif file copy to " + filename);
            PrintStream ps = null;
            BufferedReader in = null;

            try {
                in = new BufferedReader(new FileReader(token));
                ps = new PrintStream(new FileOutputStream(filename, false));
                while (in.ready()) {
                    String s = in.readLine();
                    int n = s.indexOf("{");

                    if (n == -1) {
                        ps.println(s);
                    } else {
                        boolean endOfline = false;

                        while (n != -1) {
                            ps.print(s.substring(0, n));
                            int n1 = s.indexOf("}");
                            String tok = s.substring(n + 1, n1);

                            if (tok.equals("instanceId")) {
                                ps.print(instanceId);
                            } else if (tok.equals("rootSuffix")) {
                                ps.print(baseDN);
                            } else if (tok.equals("database")) {
                                ps.print(database);
                            }
                            if ((s.length() + 1) == n1) {
                                endOfline = true;
                                break;
                            }
                            s = s.substring(n1 + 1);
                            n = s.indexOf("{");
                        }

                        if (!endOfline) {
                            ps.println(s);
                        }
                    } 
                }
                in.close();
                ps.close();
            } catch (Exception e) { 
                CMS.debug("DBSubsystem popuateDB: " + e.toString());
                throw new IOException(
                        "Problem of copying ldif file: " + filename);
            }

            LDAPUtil.importLDIF(conn, filename);
        }
    }

    /**
     * Commit parameter changes
     */
    public void update(HttpServletRequest request,
            HttpServletResponse response,
            Context context) throws IOException {
        IConfigStore cs = CMS.getConfigStore();
	boolean hasErr = false;

        boolean firsttime = false;
        context.put("firsttime", "false");
        try {
            String v = cs.getString("preop.database.removeData");
        } catch (Exception e) {
            context.put("firsttime", "true");
            firsttime = true;
        }

        String hostname1 = "";
        String portStr1 = "";
        String database1 = "";
        String basedn1 = "";

        try {
            hostname1 = cs.getString("internaldb.ldapconn.host", "");
            portStr1 = cs.getString("internaldb.ldapconn.port", "");
            database1 = cs.getString("internaldb.database", "");
            basedn1 = cs.getString("internaldb.basedn", "");
        } catch (Exception e) {
        }

        String hostname2 = HttpInput.getHostname(request, "host");
        String portStr2 = HttpInput.getPortNumber(request, "port");
        String database2 = HttpInput.getLdapDatabase(request, "database");
        String basedn2 = HttpInput.getDN(request, "basedn");

        cs.putString("internaldb.ldapconn.host", hostname2);
        cs.putString("internaldb.ldapconn.port", portStr2);
        cs.putString("internaldb.basedn", basedn2);
        String binddn = HttpInput.getDN(request, "binddn");
        cs.putString("internaldb.ldapauth.bindDN", binddn);
        cs.putString("internaldb.database", database2);
        String secure = HttpInput.getCheckbox(request, "secureConn");
        cs.putString("internaldb.ldapconn.secureConn", (secure.equals("on")?"true":"false"));
        String remove = HttpInput.getID(request, "removeData");
        if (isPanelDone() && (remove == null || remove.equals(""))) {
             /* if user submits the same data, they just want to skip 
                to the next panel, no database population is required. */
            if (hostname1.equals(hostname2) && 
                portStr1.equals(portStr2) && 
                database1.equals(database2)) {
                return;
            }
        }

        mServlet.cleanUpFromPanel(mServlet.getPanelNo(request));


        try {
            populateDB(request, context, (secure.equals("on")?"true":"false"));
        } catch (IOException e) {
            CMS.debug("DatabasePanel update: populateDB Exception: "+e.toString());
            throw e;
        } catch (Exception e) {
            CMS.debug("DatabasePanel update: populateDB Exception: "+e.toString());
            context.put("errorString", e.toString());
            cs.putString("preop.database.errorString", e.toString());
            throw new IOException(e.toString());
        }

        String bindpwd = HttpInput.getPassword(request, "__bindpwd");
        IConfigStore psStore = null;
        String passwordFile = null;

        try {
            passwordFile = cs.getString("passwordFile");
            psStore = CMS.createFileConfigStore(passwordFile);
        } catch (Exception e) {
            CMS.debug("ConfigDatabaseServlet update: " + e.toString());
            throw new IOException( e.toString() );
        }
        psStore.putString("internaldb", bindpwd);
        cs.putString("preop.database.removeData", "false");

        try {
            cs.commit(false);
            psStore.commit(false);
            CMS.reinit(IDBSubsystem.SUB_ID);
            String type = cs.getString("cs.type", "");
            if (type.equals("CA"))
                CMS.reinit(ICertificateAuthority.ID);
            CMS.reinit(IAuthSubsystem.ID);
            CMS.reinit(IAuthzSubsystem.ID);
            CMS.reinit(IUGSubsystem.ID);
        } catch (Exception e) {
            CMS.debug("DatabasePanel update: " + e.toString());
            context.put("errorString", e.toString());
            cs.putString("preop.database.errorString", e.toString());
            throw new IOException(e.toString());
        }

        String select = "";
        try {
            select = cs.getString("preop.subsystem.select", "");
        } catch (Exception e) {
        }

        if (select.equals("clone")) {
            CMS.debug("Start setting up replication.");
            setupReplication(request, context, (secure.equals("on")?"true":"false"));
            CMS.debug("Finish setting up replication.");
 
            try {
                CMS.reinit(IDBSubsystem.SUB_ID);
                CMS.reinit(IAuthSubsystem.ID);
                CMS.reinit(IAuthzSubsystem.ID);
            } catch (Exception e) {
            }
        }

        // always populate the index the last
        try {
          CMS.debug("Populating local indexes");
          LDAPConnection conn = getLocalLDAPConn(context, 
                            (secure.equals("on")?"true":"false"));
          importLDIFS("preop.internaldb.post_ldif", conn);

          /* For vlvtask, we need to check if the task has 
             been completed or not
           */
          String wait_dn = cs.getString("preop.internaldb.wait_dn", "");
          if (!wait_dn.equals("")) {
            LDAPEntry task = null;
            do {
              try {
                CMS.debug("Checking wait_dn " + wait_dn);
                task = conn.read(wait_dn, (String[])null);
                if (task != null) {
                   Thread.sleep(1000);
                }
              } catch (LDAPException e) {
                task = null;
              }
            } while (task != null);
            CMS.debug("Done checking wait_dn " + wait_dn);
          }

          conn.disconnect();
          CMS.debug("Done populating local indexes");
        } catch (Exception e) {
          CMS.debug("Populating index failure - " + e);
        }

        if (hasErr == false) {
          cs.putBoolean("preop.Database.done", true);
          try {
            cs.commit(false);
          } catch (EBaseException e) { 
            CMS.debug(
                  "DatabasePanel: update() Exception caught at config commit: "
                            + e.toString());
	  }
	}
    }

    private void setupReplication(HttpServletRequest request,
				  Context context, String secure) throws IOException {
        String bindpwd = HttpInput.getPassword(request, "__bindpwd");
        IConfigStore cs = CMS.getConfigStore();
 
        String cstype = "";
        String machinename = "";
        String instanceId = "";
        try {
            cstype = cs.getString("cs.type");
            cstype = toLowerCaseSubsystemType(cstype);
            machinename = cs.getString("machineName", "");
            instanceId = cs.getString("instanceId", "");
        } catch (Exception e) {
        }


        //setup replication agreement
        String masterAgreementName = "masterAgreement1-"+machinename+"-"+instanceId;
        cs.putString("internaldb.replication.master", masterAgreementName);
        String cloneAgreementName = "cloneAgreement1-"+machinename+"-"+instanceId;
        cs.putString("internaldb.replication.consumer", cloneAgreementName);
 
        try {
            cs.commit(false);
        } catch (Exception e) {
        }

        String master1_hostname = "";
        int master1_port = -1;
        String master1_binddn = "";
        String master1_bindpwd = "";

        try {
            master1_hostname = cs.getString("preop.internaldb.master.hostname", "");
            master1_port = cs.getInteger("preop.internaldb.master.port", -1);
            master1_binddn = cs.getString("preop.internaldb.master.binddn", "");
            master1_bindpwd = cs.getString("preop.internaldb.master.bindpwd", "");
        } catch (Exception e) {
        }

        String master2_hostname = "";
        int master2_port = -1;
        String master2_binddn = "";
        String master2_bindpwd = "";

        try {
            master2_hostname = cs.getString("internaldb.ldapconn.host", "");
            master2_port = cs.getInteger("internaldb.ldapconn.port", -1);
            master2_binddn = cs.getString("internaldb.ldapauth.bindDN", "");
            master2_bindpwd = bindpwd;
        } catch (Exception e) {
        }
  
        LDAPConnection conn1 = null;
        LDAPConnection conn2 = null;
        if (secure.equals("true")) {
          CMS.debug("DatabasePanel setupReplication: creating secure (SSL) connections for internal ldap");
          conn1 = new LDAPConnection(CMS.getLdapJssSSLSocketFactory());
          conn2 = new LDAPConnection(CMS.getLdapJssSSLSocketFactory());
	} else {
          CMS.debug("DatabasePanel setupreplication: creating non-secure (non-SSL) connections for internal ldap");
          conn1 = new LDAPConnection();
          conn2 = new LDAPConnection();
	}

        String basedn = "";
        try {
            basedn = cs.getString("internaldb.basedn");
        } catch (Exception e) {
        }

        try {
            conn1.connect(master1_hostname, master1_port, master1_binddn,
              master1_bindpwd);
            conn2.connect(master2_hostname, master2_port, master2_binddn,
              master2_bindpwd);
            String suffix = cs.getString("internaldb.basedn", "");

            String replicadn = "cn=replica,cn=\""+suffix+"\",cn=mapping tree,cn=config";
            CMS.debug("DatabasePanel setupReplication: replicadn="+replicadn);

            createReplicationManager(conn1, master1_bindpwd);
            createReplicationManager(conn2, master2_bindpwd);

            String dir1 = getInstanceDir(conn1);
            createChangeLog(conn1, dir1 + "/changelogs");

            String dir2 = getInstanceDir(conn2);
            createChangeLog(conn2, dir2 + "/changelogs");

            enableReplication(replicadn, conn1, basedn, "1");
            enableReplication(replicadn, conn2, basedn, "2");

            CMS.debug("DatabasePanel setupReplication: Finished enabling replication");

            createReplicationAgreement(replicadn, conn1, masterAgreementName, 
              master2_hostname, master2_port, master2_bindpwd, basedn);

            createReplicationAgreement(replicadn, conn2, cloneAgreementName, 
              master1_hostname, master1_port, master1_bindpwd, basedn);

            // initialize consumer
            initializeConsumer(replicadn, conn1, masterAgreementName);


            // compare entries
            compareAndWaitEntries(conn1, conn2, basedn);

        } catch (Exception e) {
            CMS.debug("DatabasePanel setupReplication: "+e.toString());
            throw new IOException("Failed to setup the replication for cloning.");
        }
    }

    private void compareAndWaitEntries(LDAPConnection conn1, 
                      LDAPConnection conn2, String basedn)
    {
        try {
           LDAPSearchResults res = conn1.search(basedn, 
                LDAPConnection.SCOPE_ONE, "(objectclass=*)", null, true);
           while (res.hasMoreElements()) {
               LDAPEntry source = res.next();
               // check if this entry is present in conn2
               LDAPEntry dest = null;
               do {
                 CMS.debug("DatabasePanel comparetAndWaitEntries checking " + 
                     source.getDN());
                 try {
                   dest = conn2.read(source.getDN(), (String[])null);
                 } catch (Exception e1) {
                   CMS.debug("DatabasePanel comparetAndWaitEntries " + 
                    source.getDN() + " not found, let's wait!");
                 try {
                   Thread.sleep(5000);
                 } catch (Exception e2) {
                 }
               }
             } while (dest == null);

             // check children of this entry
             compareAndWaitEntries(conn1, conn2, source.getDN());
          } // while
       } catch (Exception ex) {
              CMS.debug("DatabasePanel comparetAndWaitEntries " + ex);
       }
    }

    /**
     * If validiate() returns false, this method will be called.
     */
    public void displayError(HttpServletRequest request,
            HttpServletResponse response,
            Context context) {

        try {
          initParams(request, context);
        } catch (IOException e) { 
        }
        context.put("title", "Database");
        context.put("panel", "admin/console/config/databasepanel.vm");
    }

    private boolean isAgreementExist(String replicadn, LDAPConnection conn,
      String name) {
        String dn = "cn="+name+","+replicadn;
        String filter = "(cn="+name+")";
        String[] attrs = {"cn"};
        try {
            LDAPSearchResults results = conn.search(dn, LDAPv3.SCOPE_SUB,
              filter, attrs, false);
            while (results.hasMoreElements())
                return true; 
        } catch (LDAPException e) {
            return false;
        }

        return false;
    }

    private void createReplicationManager(LDAPConnection conn, String pwd)
      throws LDAPException {
        LDAPAttributeSet attrs = null;
        LDAPEntry entry = null;
        String dn = "cn=Replication Manager,cn=config";
        try {
            attrs = new LDAPAttributeSet();
            attrs.add(new LDAPAttribute("objectclass", "top"));
            attrs.add(new LDAPAttribute("objectclass", "person"));
            attrs.add(new LDAPAttribute("userpassword", pwd));
            attrs.add(new LDAPAttribute("cn", "Replication Manager"));
            attrs.add(new LDAPAttribute("sn", "manager"));
            entry = new LDAPEntry("cn=Replication Manager,cn=config",
              attrs);
            conn.add(entry);
        } catch (LDAPException e) {
            if (e.getLDAPResultCode() == LDAPException.ENTRY_ALREADY_EXISTS) {
                CMS.debug("DatabasePanel createReplicationManager: Replication Manager has already used");
                try {
                    conn.delete(dn);
                    conn.add(entry);
                } catch (LDAPException ee) {
                    CMS.debug("DatabasePanel createReplicationManager: "+ee.toString());
                }
                return;
            } else {
                CMS.debug("DatabasePanel createReplicationManager: Failed to create replication manager. Exception: "+e.toString());
                throw e;
            }
        }

        CMS.debug("DatabasePanel createReplicationManager: Successfully create Replication Manager");
    }

    private void createChangeLog(LDAPConnection conn, String dir)
      throws LDAPException {
        LDAPAttributeSet attrs = null;
        LDAPEntry entry = null;
        String dn = "cn=changelog5,cn=config";
        try {
            attrs = new LDAPAttributeSet();
            attrs.add(new LDAPAttribute("objectclass", "top"));
            attrs.add(new LDAPAttribute("objectclass", "extensibleObject"));
            attrs.add(new LDAPAttribute("cn", "changelog5"));
            attrs.add(new LDAPAttribute("nsslapd-changelogdir", dir));
            entry = new LDAPEntry("cn=changelog5,cn=config", attrs);
            conn.add(entry);
        } catch (LDAPException e) {
            if (e.getLDAPResultCode() == LDAPException.ENTRY_ALREADY_EXISTS) {
                CMS.debug("DatabasePanel createChangeLog: Changelog entry has already used");
/* leave it, dont delete it because it will have operation error
                try {
                    conn.delete(dn);
                    conn.add(entry);
                } catch (LDAPException ee) {
                    CMS.debug("DatabasePanel createChangeLog: "+ee.toString());
                }
*/
                return;
            } else {
                CMS.debug("DatabasePanel createChangeLog: Failed to create changelog entry. Exception: "+e.toString());
                throw e;
            }
        }

        CMS.debug("DatabasePanel createChangeLog: Successfully create change log entry");
    }

    private void enableReplication(String replicadn, LDAPConnection conn, String basedn, String id)
      throws LDAPException {
        CMS.debug("DatabasePanel enableReplication: replicadn: "+replicadn);
        LDAPAttributeSet attrs = null;
        LDAPEntry entry = null;
        try {
            attrs = new LDAPAttributeSet();
            attrs.add(new LDAPAttribute("objectclass", "top"));
            attrs.add(new LDAPAttribute("objectclass", "nsDS5Replica"));
            attrs.add(new LDAPAttribute("objectclass", "extensibleobject"));
            attrs.add(new LDAPAttribute("nsDS5ReplicaRoot", basedn));
            attrs.add(new LDAPAttribute("nsDS5ReplicaType", "3"));
            attrs.add(new LDAPAttribute("nsDS5ReplicaBindDN",
              "cn=replication manager,cn=config"));
            attrs.add(new LDAPAttribute("cn", "replica"));
            attrs.add(new LDAPAttribute("nsDS5ReplicaId", id));
            attrs.add(new LDAPAttribute("nsds5flags", "1"));
            entry = new LDAPEntry(replicadn, attrs);
            conn.add(entry);
        } catch (LDAPException e) {
            if (e.getLDAPResultCode() == LDAPException.ENTRY_ALREADY_EXISTS) {
                CMS.debug("DatabasePanel enableReplication: "+replicadn+" has already used");
                try {
                    conn.delete(replicadn);
                    conn.add(entry);
                } catch (LDAPException ee) {
                }
                return;
            } else {
                CMS.debug("DatabasePanel enableReplication: Failed to create "+replicadn+" entry. Exception: "+e.toString());
                return;
            }
        }

        CMS.debug("DatabasePanel enableReplication: Successfully create "+replicadn+" entry.");
    }

    private void createReplicationAgreement(String replicadn, 
      LDAPConnection conn, String name, String replicahost, int replicaport, 
      String replicapwd, String basedn) throws LDAPException {
        String dn = "cn="+name+","+replicadn;
        CMS.debug("DatabasePanel createReplicationAgreement: dn: "+dn);
        LDAPEntry entry = null;
        LDAPAttributeSet attrs = null;
        try {
            attrs = new LDAPAttributeSet();
            attrs.add(new LDAPAttribute("objectclass", "top"));
            attrs.add(new LDAPAttribute("objectclass",
              "nsds5replicationagreement"));
            attrs.add(new LDAPAttribute("cn", name));
            attrs.add(new LDAPAttribute("nsDS5ReplicaRoot", basedn));
            attrs.add(new LDAPAttribute("nsDS5ReplicaHost", replicahost));
            attrs.add(new LDAPAttribute("nsDS5ReplicaPort", ""+replicaport));
            attrs.add(new LDAPAttribute("nsDS5ReplicaBindDN",
              "cn=replication manager,cn=config"));
            attrs.add(new LDAPAttribute("nsDS5ReplicaBindMethod", "Simple"));
            attrs.add(new LDAPAttribute("nsds5replicacredentials", replicapwd));
            CMS.debug("About to set description attr to " + name);
            attrs.add(new LDAPAttribute("description",name));

            entry = new LDAPEntry(dn, attrs);
            conn.add(entry);
        } catch (LDAPException e) {
            if (e.getLDAPResultCode() == LDAPException.ENTRY_ALREADY_EXISTS) {
                CMS.debug("DatabasePanel createReplicationAgreement: "+dn+" has already used");
                try {
                    conn.delete(dn);
                } catch (LDAPException ee) {
                    CMS.debug("DatabasePanel createReplicationAgreement: "+ee.toString());
                    throw ee;
                }

                try {
                    conn.add(entry);
                } catch (LDAPException ee) {
                    CMS.debug("DatabasePanel createReplicationAgreement: "+ee.toString());
                    throw ee;
                }
            } else {
                CMS.debug("DatabasePanel createReplicationAgreement: Failed to create "+dn+" entry. Exception: "+e.toString());
                throw e;
            }
        }

        CMS.debug("DatabasePanel createReplicationAgreement: Successfully create replication agreement "+name);
    }

    private void initializeConsumer(String replicadn, LDAPConnection conn, 
      String name) {
        String dn = "cn="+name+","+replicadn;
        CMS.debug("DatabasePanel initializeConsumer: initializeConsumer dn: "+dn);
        CMS.debug("DatabasePanel initializeConsumer: initializeConsumer host: "+conn.getHost() + " port: " + conn.getPort());
        try {
            LDAPAttribute attr = new LDAPAttribute("nsds5beginreplicarefresh",
              "start");
            LDAPModification mod = new LDAPModification(
              LDAPModification.REPLACE, attr);
            CMS.debug("DatabasePanel initializeConsumer: start modifying");
            conn.modify(dn, mod);
            CMS.debug("DatabasePanel initializeConsumer: Finish modification.");
        } catch (LDAPException e) {
            CMS.debug("DatabasePanel initializeConsumer: Failed to modify "+dn+" entry. Exception: "+e.toString());
            return;
        } catch (Exception e) {
            CMS.debug("DatabasePanel initializeConsumer: exception " + e);
        }

        try {
            CMS.debug("DatabasePanel initializeConsumer: thread sleeping for 5 seconds.");
            Thread.sleep(5000);
            CMS.debug("DatabasePanel initializeConsumer: finish sleeping.");
        } catch (InterruptedException ee) {
            CMS.debug("DatabasePanel initializeConsumer: exception: "+ee.toString());
        }

        CMS.debug("DatabasePanel initializeConsumer: Successfully initialize consumer");
    }

    private String getInstanceDir(LDAPConnection conn) {
        String instancedir="";
        try {
            String filter = "(objectclass=*)";
            String[] attrs = {"nsslapd-directory"};
            LDAPSearchResults results = conn.search("cn=config,cn=ldbm database,cn=plugins,cn=config", LDAPv3.SCOPE_SUB,
              filter, attrs, false);

            while (results.hasMoreElements()) {
                LDAPEntry entry = results.next();
                String dn = entry.getDN();
                CMS.debug("DatabasePanel getInstanceDir: DN for storing nsslapd-directory: "+dn);
                LDAPAttributeSet entryAttrs = entry.getAttributeSet();
                Enumeration attrsInSet = entryAttrs.getAttributes();
                while (attrsInSet.hasMoreElements()) {
                    LDAPAttribute nextAttr = (LDAPAttribute)attrsInSet.nextElement();
                    String attrName = nextAttr.getName();
                    CMS.debug("DatabasePanel getInstanceDir: attribute name: "+attrName);
                    Enumeration valsInAttr = nextAttr.getStringValues();
                    while ( valsInAttr.hasMoreElements() ) {
                        String nextValue = (String)valsInAttr.nextElement();
                        if (attrName.equalsIgnoreCase("nsslapd-directory")) {
                            CMS.debug("DatabasePanel getInstanceDir: instanceDir="+nextValue);
                            return nextValue.substring(0,nextValue.lastIndexOf("/db"));
                        }
                    }
                }
            }
        } catch (LDAPException e) {
            CMS.debug("DatabasePanel getInstanceDir: Error in retrieving the instance directory. Exception: "+e.toString());
        }

        return instancedir;
    }
}
