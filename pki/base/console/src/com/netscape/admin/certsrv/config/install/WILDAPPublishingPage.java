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
package com.netscape.admin.certsrv.config.install;

import java.awt.*;
import javax.swing.*;
import com.netscape.admin.certsrv.*;
import com.netscape.admin.certsrv.connection.*;
import com.netscape.admin.certsrv.wizard.*;
import com.netscape.certsrv.common.*;

/**
 * Introduction page for installation wizard.
 *
 * @author Christine Ho
 * @version $Revision: 14593 $, $Date: 2007-05-01 16:35:45 -0700 (Tue, 01 May 2007) $
 * @see com.netscape.admin.certsrv.config.install
 */
class WILDAPPublishingPage extends WizardBasePanel implements IWizardPanel {
    private JTextField mHostNameText, mPortText, mBindAsText;
    private JCheckBox mSecurePort, mEnable;
    private JLabel mBindAsLabel, mCertLabel; 
    private JComboBox mAuthBox, mCertBox, mVersionBox;

    private static final String PANELNAME = "LDAPPUBLISHINGWIZARD";
    private static final String HELPINDEX =
      "configuration-kra-wizard-change-keyscheme-help";
    private static final String EMPTYSTR = "                    ";
    private static final String DELIMITER = ",";
    private final static String[] AUTHTYPE = {Constants.PR_BASIC_AUTH,
      Constants.PR_SSL_AUTH};

    WILDAPPublishingPage() {
        super(PANELNAME);
        init();
    }

    public boolean isLastPage() {
        return false;
    }

    public boolean initializePanel(WizardInfo info) {
        return true; 
    }

    public boolean validatePanel() {
        return true;
    }

    public boolean concludePanel(WizardInfo info) {
        return true;
    }

    public void callHelp() {
        CMSAdminUtil.help(HELPINDEX);
    }

    protected void init() {
        GridBagLayout gb = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        setLayout(gb);

        CMSAdminUtil.resetGBC(gbc);
        JTextArea desc = createTextArea(
          CMSAdminUtil.wrapText(mResource.getString(
            "INTERNALDBWIZARD_TEXT_HEADING_LABEL"), 80), 2, 80);
        gbc.anchor = gbc.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;
        gbc.insets = new Insets(COMPONENT_SPACE,COMPONENT_SPACE,
          COMPONENT_SPACE,COMPONENT_SPACE);
        gbc.gridwidth = gbc.REMAINDER;
        add(desc, gbc);

        mEnable = makeJCheckBox("ENABLE");
        CMSAdminUtil.resetGBC(gbc);
        gbc.anchor = gbc.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(COMPONENT_SPACE,COMPONENT_SPACE, 0,
          COMPONENT_SPACE);
        gbc.gridwidth = gbc.REMAINDER;
        add(mEnable, gbc);

        JPanel panel = new JPanel();
        panel.setBorder(CMSAdminUtil.makeTitledBorder(mResource,
          PANELNAME, "DESTINATION"));
        GridBagLayout gb1 = new GridBagLayout();
        panel.setLayout(gb1);

        CMSAdminUtil.resetGBC(gbc);
        gbc.anchor = gbc.NORTHWEST;
        gbc.insets = new Insets(COMPONENT_SPACE,COMPONENT_SPACE, 0,
          COMPONENT_SPACE);
        gbc.weighty = 1.0;
        gbc.weightx = 1.0;
        gbc.gridwidth = gbc.REMAINDER;
        add(panel, gbc);

        CMSAdminUtil.resetGBC(gbc);
        JLabel hostName = makeJLabel("HOST");
        gbc.anchor = gbc.NORTHEAST;
        gbc.fill = gbc.NONE;
        gbc.insets = new Insets(COMPONENT_SPACE,COMPONENT_SPACE, 0,
          COMPONENT_SPACE);
        panel.add(hostName, gbc);
        
        CMSAdminUtil.resetGBC(gbc);
        mHostNameText = makeJTextField(30);
        gbc.anchor = gbc.NORTHWEST;
        gbc.fill = gbc.NONE;
        gbc.insets = new Insets(COMPONENT_SPACE,COMPONENT_SPACE, 0,
          COMPONENT_SPACE);
        gbc.gridwidth = gbc.REMAINDER;
        panel.add(mHostNameText, gbc);
        
/*
        CMSAdminUtil.resetGBC(gbc);
        JTextArea dummy = createTextArea(" ", 2, 5);
        gbc.anchor = gbc.NORTHWEST;
        gbc.gridwidth = gbc.REMAINDER;
        gbc.weightx = 1.0;
        add(dummy, gbc);
*/

        CMSAdminUtil.resetGBC(gbc);
        JLabel portNumber = makeJLabel("PORT");        
        gbc.anchor = gbc.NORTHEAST;
        gbc.fill = gbc.NONE;
        gbc.insets = new Insets(COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE,COMPONENT_SPACE);
        panel.add(portNumber, gbc);
 
        CMSAdminUtil.resetGBC(gbc);
        mPortText = makeJTextField(10);
        gbc.anchor = gbc.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE,COMPONENT_SPACE);
        panel.add(mPortText, gbc);
 
        CMSAdminUtil.resetGBC(gbc);
        mSecurePort = makeJCheckBox("SECUREPORT");
        gbc.anchor = gbc.NORTHWEST;
        //gbc.weightx = 0.0;
        gbc.gridwidth = gbc.REMAINDER;
        gbc.fill = gbc.NONE;
        gbc.insets = new Insets(COMPONENT_SPACE,
          DIFFERENT_COMPONENT_SPACE - COMPONENT_SPACE,0,COMPONENT_SPACE);
        panel.add(mSecurePort, gbc);

        CMSAdminUtil.resetGBC(gbc);
        JLabel versionLbl = makeJLabel("VERSION");
        gbc.anchor = gbc.NORTHEAST;
        gbc.fill = gbc.NONE;
        gbc.insets = new Insets(0, COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE);
        panel.add(versionLbl, gbc);

        CMSAdminUtil.resetGBC(gbc);
        mVersionBox = makeJComboBox("VERSION");
        gbc.anchor = gbc.NORTHWEST;
        gbc.fill = gbc.NONE;
        gbc.insets = new Insets(0, COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE);
        panel.add(mVersionBox, gbc);

        CMSAdminUtil.resetGBC(gbc);
        JLabel dummy = new JLabel(" ");
        gbc.anchor = gbc.NORTHWEST;
        gbc.weightx = 1.0;
        gbc.gridwidth = gbc.REMAINDER;
        gbc.insets = new Insets(0, COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE);
        panel.add(dummy, gbc);

        CMSAdminUtil.resetGBC(gbc);
        mBindAsLabel = makeJLabel("BINDAS");
        gbc.anchor = gbc.NORTHEAST;
        gbc.fill = gbc.NONE;
        gbc.insets = new Insets(0, COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE);
        panel.add(mBindAsLabel, gbc);

        CMSAdminUtil.resetGBC(gbc);
        mBindAsText = makeJTextField(30);
        gbc.anchor = gbc.NORTHWEST;
        gbc.fill = gbc.NONE;
        gbc.gridwidth = gbc.REMAINDER;
        gbc.insets = new Insets(0, COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE);
        panel.add(mBindAsText, gbc);

        CMSAdminUtil.resetGBC(gbc);
        mCertLabel = makeJLabel("CERTLIST");
        gbc.anchor = gbc.NORTHEAST;
        gbc.fill = gbc.NONE;
        gbc.insets = new Insets(0, COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE);
        panel.add(mCertLabel, gbc);

        CMSAdminUtil.resetGBC(gbc);
        mCertBox = new JComboBox();
        mCertBox.addItem("internal");
        gbc.fill = gbc.NONE;
        gbc.anchor = gbc.NORTHWEST;
        gbc.insets = new Insets(0, COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE);
        panel.add(mCertBox, gbc);

        CMSAdminUtil.resetGBC(gbc);
        JLabel dummy1 = new JLabel(" ");
        gbc.gridwidth = gbc.REMAINDER;
        gbc.weightx = 1.0;
        gbc.insets = new Insets(0, COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE);
        panel.add(dummy1, gbc);

        CMSAdminUtil.resetGBC(gbc);
        JLabel authLbl = makeJLabel("AUTHTYPE");
        gbc.anchor = gbc.NORTHEAST;
        gbc.fill = gbc.NONE;
        gbc.gridheight = gbc.REMAINDER;
        //gbc.weighty = 1.0;
        gbc.insets = new Insets(0, COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE);
        panel.add(authLbl, gbc);

        CMSAdminUtil.resetGBC(gbc);
        mAuthBox = makeJComboBox("AUTHTYPE");
        //gbc.weighty = 1.0;
        gbc.anchor = gbc.NORTHWEST;
        gbc.gridheight = gbc.REMAINDER;
        gbc.insets = new Insets(0, COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE);
        panel.add(mAuthBox, gbc);

        CMSAdminUtil.resetGBC(gbc);
        JLabel dummy2 = new JLabel(" ");
        gbc.gridwidth = gbc.REMAINDER;
        gbc.anchor = gbc.NORTHWEST;
        gbc.gridheight = gbc.REMAINDER;
        gbc.weightx = 1.0;
        //gbc.weighty = 1.0;
        gbc.insets = new Insets(0, COMPONENT_SPACE, COMPONENT_SPACE,
          COMPONENT_SPACE);
        panel.add(dummy2, gbc);
       
/*
        CMSAdminUtil.resetGBC(gbc);
        JTextArea dummy1 = createTextArea(" ", 2, 30);
        gbc.anchor = gbc.NORTHWEST;
        gbc.gridwidth = gbc.REMAINDER;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.gridwidth = gbc.REMAINDER;
        add(dummy1, gbc);
*/

        super.init();
    }

    public void getUpdateInfo(WizardInfo info) {
    }
}
