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
package com.netscape.kra;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.util.StringTokenizer;
import java.util.Vector;

import netscape.security.provider.RSAPublicKey;
import netscape.security.util.BigInt;
import netscape.security.util.DerInputStream;
import netscape.security.util.DerOutputStream;
import netscape.security.util.DerValue;
import netscape.security.x509.CertificateSubjectName;
import netscape.security.x509.CertificateX509Key;
import netscape.security.x509.X509CertInfo;
import netscape.security.x509.X509Key;

import org.mozilla.jss.asn1.ANY;
import org.mozilla.jss.asn1.ASN1Util;
import org.mozilla.jss.asn1.ASN1Value;
import org.mozilla.jss.asn1.BIT_STRING;
import org.mozilla.jss.asn1.InvalidBERException;
import org.mozilla.jss.asn1.OBJECT_IDENTIFIER;
import org.mozilla.jss.asn1.OCTET_STRING;
import org.mozilla.jss.asn1.SEQUENCE;
import org.mozilla.jss.asn1.SET;
import org.mozilla.jss.pkix.cms.EncryptedContentInfo;
import org.mozilla.jss.pkix.cms.EnvelopedData;
import org.mozilla.jss.pkix.cms.RecipientInfo;
import org.mozilla.jss.pkix.crmf.CertReqMsg;
import org.mozilla.jss.pkix.crmf.CertRequest;
import org.mozilla.jss.pkix.crmf.EncryptedKey;
import org.mozilla.jss.pkix.crmf.EncryptedValue;
import org.mozilla.jss.pkix.crmf.PKIArchiveOptions;
import org.mozilla.jss.pkix.primitive.AVA;
import org.mozilla.jss.pkix.primitive.AlgorithmIdentifier;

import com.netscape.certsrv.apps.CMS;
import com.netscape.certsrv.authentication.AuthToken;
import com.netscape.certsrv.base.EBaseException;
import com.netscape.certsrv.base.SessionContext;
import com.netscape.certsrv.dbs.keydb.IKeyRepository;
import com.netscape.certsrv.kra.EKRAException;
import com.netscape.certsrv.kra.IKeyRecoveryAuthority;
import com.netscape.certsrv.kra.ProofOfArchival;
import com.netscape.certsrv.logging.AuditFormat;
import com.netscape.certsrv.logging.ILogger;
import com.netscape.certsrv.profile.IEnrollProfile;
import com.netscape.certsrv.request.IRequest;
import com.netscape.certsrv.request.IService;
import com.netscape.certsrv.security.IStorageKeyUnit;
import com.netscape.certsrv.security.ITransportKeyUnit;
import com.netscape.certsrv.util.IStatsSubsystem;
import com.netscape.cmscore.crmf.CRMFParser;
import com.netscape.cmscore.crmf.PKIArchiveOptionsContainer;
import com.netscape.cmscore.dbs.KeyRecord;


/**
 * A class represents archival request processor. It 
 * passes the request to the policy processor, and 
 * process the request according to the policy decision.
 * <P>
 * If policy returns ACCEPTED, the request will be
 * processed immediately.
 * <P>
 * Upon processing, the incoming user key is unwrapped
 * with the transport key of KRA, and then wrapped
 * with the storage key. The encrypted key is stored
 * in the internal database for long term storage.
 * <P>
 *
 * @author thomask (original)
 * @author cfu (non-RSA keys; private keys secure handling);
 * @version $Revision$, $Date$
 */
public class EnrollmentService implements IService {

    // constants
    public static final String CRMF_REQUEST = "CRMFRequest";
    public final static String ATTR_KEY_RECORD = "keyRecord";
    public final static String ATTR_PROOF_OF_ARCHIVAL = 	
        "proofOfArchival";

    // private 
    private IKeyRecoveryAuthority mKRA = null;
    private ITransportKeyUnit mTransportUnit = null;
    private IStorageKeyUnit mStorageUnit = null;
    private ILogger mSignedAuditLogger = CMS.getSignedAuditLogger();


    private final static byte EOL[] = { Character.LINE_SEPARATOR };
    private final static String
        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST =
        "LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST_4";
    private final static String
        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST_PROCESSED =
        "LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST_PROCESSED_3";
    private final static String LOGGING_SIGNED_AUDIT_KEY_RECOVERY_REQUEST =
        "LOGGING_SIGNED_AUDIT_KEY_RECOVERY_REQUEST_4";
    private final static String LOGGING_SIGNED_AUDIT_KEY_RECOVERY_REQUEST_PROCESSED =
        "LOGGING_SIGNED_AUDIT_KEY_RECOVERY_REQUEST_PROCESSED_4";
    /**
     * Constructs request processor.
     * <P>
     * 
     * @param kra key recovery authority
     */
    public EnrollmentService(IKeyRecoveryAuthority kra) {
        mKRA = kra;
        mTransportUnit = kra.getTransportKeyUnit();
        mStorageUnit = kra.getStorageKeyUnit();
    }

    public PKIArchiveOptions toPKIArchiveOptions(byte options[]) {
        ByteArrayInputStream bis = new ByteArrayInputStream(options);
        PKIArchiveOptions archOpts = null;

        try {
            archOpts = (PKIArchiveOptions)
                    (new PKIArchiveOptions.Template()).decode(bis);
        } catch (Exception e) {
            CMS.debug("EnrollProfile: getPKIArchiveOptions " + e.toString());
        }
        return archOpts;
    }
	
    /**
     * Services an enrollment/archival request.
     * <P>
     *
     * @param request enrollment request
     * @return serving successful or not
     * @exception EBaseException failed to serve
     */
    public boolean serviceRequest(IRequest request) 
        throws EBaseException {

        IStatsSubsystem statsSub = (IStatsSubsystem)CMS.getSubsystem("stats");
        if (statsSub != null) {
          statsSub.startTiming("archival", true /* main action */);
        }

        String auditMessage = null;
        String auditSubjectID = auditSubjectID();
        String auditRequesterID = auditRequesterID();
        String auditArchiveID = ILogger.UNIDENTIFIED;
        String auditPublicKey = ILogger.UNIDENTIFIED;

        String id = request.getRequestId().toString();
        if (id != null) {
            auditArchiveID = id.trim();
        }
        if (CMS.debugOn())
            CMS.debug("EnrollmentServlet: KRA services enrollment request");

        SessionContext sContext = SessionContext.getContext();
        String agentId = (String) sContext.get(SessionContext.USER_ID);
        AuthToken authToken = (AuthToken) sContext.get(SessionContext.AUTH_TOKEN);

        mKRA.log(ILogger.LL_INFO, "KRA services enrollment request");
        // unwrap user key with transport
        byte unwrapped[] = null;
        PKIArchiveOptionsContainer aOpts[] = null;

        String profileId = request.getExtDataInString("profileId");

        if (profileId == null || profileId.equals("")) {
            try {
                aOpts = CRMFParser.getPKIArchiveOptions(
                            request.getExtDataInString(IRequest.HTTP_PARAMS, CRMF_REQUEST));
            } catch (IOException e) {

                auditMessage = CMS.getLogMessage(
                        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST,
                        auditSubjectID,
                        ILogger.FAILURE,
                        auditRequesterID,
                        auditArchiveID);

                audit(auditMessage);
                throw new EKRAException(
                        CMS.getUserMessage("CMS_KRA_INVALID_PRIVATE_KEY"));
            }
        } else {
            // profile-based request
            PKIArchiveOptions options = (PKIArchiveOptions)
                toPKIArchiveOptions(
                    request.getExtDataInByteArray(IEnrollProfile.REQUEST_ARCHIVE_OPTIONS));

            aOpts = new PKIArchiveOptionsContainer[1];
            aOpts[0] = new PKIArchiveOptionsContainer(options, 
                        0/* not matter */);

            request.setExtData("dbStatus", "NOT_UPDATED");
        } 

        for (int i = 0; i < aOpts.length; i++) {
            ArchiveOptions opts = new ArchiveOptions(aOpts[i].mAO);

            if (statsSub != null) {
              statsSub.startTiming("decrypt_user_key");
            }
            mKRA.log(ILogger.LL_INFO, "KRA decrypts external private");
            if (CMS.debugOn())
               CMS.debug("EnrollmentService::about to decryptExternalPrivate");
            unwrapped = mTransportUnit.decryptExternalPrivate(
                        opts.getEncSymmKey(), 
                        opts.getSymmAlgOID(), 
                        opts.getSymmAlgParams(), 
                        opts.getEncValue());
            if (statsSub != null) {
              statsSub.endTiming("decrypt_user_key");
            }
            if (CMS.debugOn())
               CMS.debug("EnrollmentService::finished decryptExternalPrivate");
            if (unwrapped == null) {
                mKRA.log(ILogger.LL_FAILURE, CMS.getLogMessage("CMSCORE_KRA_UNWRAP_USER_KEY"));

                auditMessage = CMS.getLogMessage(
                        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST,
                        auditSubjectID,
                        ILogger.FAILURE,
                        auditRequesterID,
                        auditArchiveID);

                audit(auditMessage);
                throw new EKRAException(
                        CMS.getUserMessage("CMS_KRA_INVALID_PRIVATE_KEY"));
            }

            // retrieve pubic key
            X509Key publicKey = getPublicKey(request, aOpts[i].mReqPos);
            byte publicKeyData[] = publicKey.getEncoded();

            if (publicKeyData == null) {
                mKRA.log(ILogger.LL_FAILURE, 
                    CMS.getLogMessage("CMSCORE_KRA_PUBLIC_NOT_FOUND"));


                auditMessage = CMS.getLogMessage(
                        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST,
                        auditSubjectID,
                        ILogger.FAILURE,
                        auditRequesterID,
                        auditArchiveID);

                audit(auditMessage);
                throw new EKRAException(
                        CMS.getUserMessage("CMS_KRA_INVALID_PUBLIC_KEY"));
            }

            /* Bugscape #54948 - verify public and private key before archiving key */

            if (statsSub != null) {
              statsSub.startTiming("verify_key");
            }
            if (verifyKeyPair(publicKeyData, unwrapped) == false) {
                mKRA.log(ILogger.LL_FAILURE, 
                    CMS.getLogMessage("CMSCORE_KRA_PUBLIC_NOT_FOUND"));


                auditMessage = CMS.getLogMessage(
                        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST,
                        auditSubjectID,
                        ILogger.FAILURE,
                        auditRequesterID,
                        auditArchiveID);

                audit(auditMessage);
                throw new EKRAException(
                        CMS.getUserMessage("CMS_KRA_INVALID_PUBLIC_KEY"));
            }
            if (statsSub != null) {
              statsSub.endTiming("verify_key");
            }

            /**
             mTransportKeyUnit.verify(pKey, unwrapped);
             **/
            // retrieve owner name
            String owner = getOwnerName(request, aOpts[i].mReqPos);

            if (owner == null) {
                mKRA.log(ILogger.LL_FAILURE, CMS.getLogMessage("CMSCORE_KRA_OWNER_NAME_NOT_FOUND"));

                auditMessage = CMS.getLogMessage(
                        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST,
                        auditSubjectID,
                        ILogger.FAILURE,
                        auditRequesterID,
                        auditArchiveID);

                audit(auditMessage);
                throw new EKRAException(CMS.getUserMessage("CMS_KRA_INVALID_KEYRECORD"));
            }

            //
            // privateKeyData ::= SEQUENCE {
            //                       sessionKey OCTET_STRING,
            //                       encKey OCTET_STRING,
            //                    }
            //
            mKRA.log(ILogger.LL_INFO, "KRA encrypts internal private");
            if (statsSub != null) {
              statsSub.startTiming("encrypt_user_key");
            }
            byte privateKeyData[] = mStorageUnit.encryptInternalPrivate(
                    unwrapped);
            if (statsSub != null) {
              statsSub.endTiming("encrypt_user_key");
            }

            if (privateKeyData == null) {
                mKRA.log(ILogger.LL_FAILURE, CMS.getLogMessage("CMSCORE_KRA_WRAP_USER_KEY"));

                auditMessage = CMS.getLogMessage(
                        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST,
                        auditSubjectID,
                        ILogger.FAILURE,
                        auditRequesterID,
                        auditArchiveID);

                audit(auditMessage);
                throw new EKRAException(
                        CMS.getUserMessage("CMS_KRA_INVALID_PRIVATE_KEY"));
            }

            // create key record
            KeyRecord rec = new KeyRecord(null, publicKeyData, 
                    privateKeyData, owner, 
                    publicKey.getAlgorithmId().getOID().toString(), agentId);

            // we deal with RSA key only
            try {
                RSAPublicKey rsaPublicKey = new RSAPublicKey(publicKeyData);

                rec.setKeySize(Integer.valueOf(rsaPublicKey.getKeySize()));
            } catch (InvalidKeyException e) {

                auditMessage = CMS.getLogMessage(
                        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST,
                        auditSubjectID,
                        ILogger.FAILURE,
                        auditRequesterID,
                        auditArchiveID);

                audit(auditMessage);
                throw new EKRAException(CMS.getUserMessage("CMS_KRA_INVALID_KEYRECORD"));
            }

            
            // if record alreay has a serial number, yell out.
            if (rec.getSerialNumber() != null) {
                mKRA.log(ILogger.LL_FAILURE, 
                    CMS.getLogMessage("CMSCORE_KRA_INVALID_SERIAL_NUMBER",
                        rec.getSerialNumber().toString()));


                auditMessage = CMS.getLogMessage(
                        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST,
                        auditSubjectID,
                        ILogger.FAILURE,
                        auditRequesterID,
                        auditArchiveID);

                audit(auditMessage);
                throw new EKRAException(CMS.getUserMessage("CMS_KRA_INVALID_STATE"));
            }
            IKeyRepository storage = mKRA.getKeyRepository();
            BigInteger serialNo = storage.getNextSerialNumber();

            if (serialNo == null) {
                mKRA.log(ILogger.LL_FAILURE, 
                    CMS.getLogMessage("CMSCORE_KRA_GET_NEXT_SERIAL"));

                auditMessage = CMS.getLogMessage(
                        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST,
                        auditSubjectID,
                        ILogger.FAILURE,
                        auditRequesterID,
                        auditArchiveID);

                audit(auditMessage);
                throw new EKRAException(CMS.getUserMessage("CMS_KRA_INVALID_STATE"));
            }
            if (i == 0) {
                rec.set(KeyRecord.ATTR_ID, serialNo);
                request.setExtData(ATTR_KEY_RECORD, serialNo);
            } else {
                rec.set(KeyRecord.ATTR_ID + i, serialNo);
                request.setExtData(ATTR_KEY_RECORD + i, serialNo);
            }

            mKRA.log(ILogger.LL_INFO, "KRA adding key record " + serialNo);
            if (statsSub != null) {
              statsSub.startTiming("store_key");
            }
            storage.addKeyRecord(rec);
            if (statsSub != null) {
              statsSub.endTiming("store_key");
            }
	
            if (CMS.debugOn())
                CMS.debug("EnrollmentService: key record 0x" + serialNo.toString(16)
                    + " (" + owner + ") archived");

            mKRA.log(ILogger.LL_INFO, "key record 0x" + 
                serialNo.toString(16)
                + " (" + owner + ") archived");

            // for audit log
            String authMgr = AuditFormat.NOAUTH;
	        
            if (authToken != null) {
                authMgr =
                        authToken.getInString(AuthToken.TOKEN_AUTHMGR_INST_NAME);
            }
            CMS.getLogger().log(ILogger.EV_AUDIT,
                ILogger.S_KRA,
                AuditFormat.LEVEL,
                AuditFormat.FORMAT,
                new Object[] {
                    IRequest.KEYARCHIVAL_REQUEST,
                    request.getRequestId(),
                    AuditFormat.FROMAGENT + " agentID: " + agentId,
                    authMgr,
                    "completed",
                    owner,
                    "serial number: 0x" + serialNo.toString(16)}
            );

           
            // store a message in the signed audit log file
            auditMessage = CMS.getLogMessage(
                        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST,
                        auditSubjectID,
                        ILogger.SUCCESS,
                        auditRequesterID,
                        auditArchiveID);

            audit(auditMessage);

            // store a message in the signed audit log file
            auditPublicKey = auditPublicKey(rec);
            auditMessage = CMS.getLogMessage(
                        LOGGING_SIGNED_AUDIT_PRIVATE_KEY_ARCHIVE_REQUEST_PROCESSED,
                        auditSubjectID,
                        ILogger.SUCCESS,
                        auditPublicKey);

            audit(auditMessage);

            // Xxx - should sign this proof of archival
            ProofOfArchival mProof = new ProofOfArchival(serialNo,
                    owner, mKRA.getX500Name().toString(),
                    rec.getCreateTime());

            DerOutputStream mProofOut = new DerOutputStream();
            mProof.encode(mProofOut);
            if (i == 0) {
                request.setExtData(ATTR_PROOF_OF_ARCHIVAL,
                        mProofOut.toByteArray());
            } else {
                request.setExtData(ATTR_PROOF_OF_ARCHIVAL + i,
                        mProofOut.toByteArray());
            }
		
        } // for

        /*
         request.delete(IEnrollProfile.REQUEST_SUBJECT_NAME);
         request.delete(IEnrollProfile.REQUEST_EXTENSIONS);
         request.delete(IEnrollProfile.REQUEST_VALIDITY);
         request.delete(IEnrollProfile.REQUEST_KEY);
         request.delete(IEnrollProfile.REQUEST_SIGNING_ALGORITHM);
         request.delete(IEnrollProfile.REQUEST_LOCALE);
         */

        request.setExtData(IRequest.RESULT, IRequest.RES_SUCCESS);

        // update request
        mKRA.log(ILogger.LL_INFO, "KRA updating request");
        mKRA.getRequestQueue().updateRequest(request);

        if (statsSub != null) {
          statsSub.endTiming("archival");
        }
		
        return true;
    }

    public boolean verifyKeyPair(byte publicKeyData[],  byte privateKeyData[]) 
    {
      try {
          DerValue publicKeyVal = new DerValue(publicKeyData); 
          DerInputStream publicKeyIn = publicKeyVal.data;
          publicKeyIn.getSequence(0);
          DerValue publicKeyDer = new DerValue(publicKeyIn.getBitString()); 
          DerInputStream publicKeyDerIn = publicKeyDer.data;
          BigInt publicKeyModulus = publicKeyDerIn.getInteger();
          BigInt publicKeyExponent = publicKeyDerIn.getInteger();

          DerValue privateKeyVal = new DerValue(privateKeyData); 
          if (privateKeyVal.tag != DerValue.tag_Sequence) 
              return false;
          DerInputStream privateKeyIn = privateKeyVal.data;
          privateKeyIn.getInteger();
          privateKeyIn.getSequence(0);
          DerValue privateKeyDer = new DerValue(privateKeyIn.getOctetString()); 
          DerInputStream privateKeyDerIn = privateKeyDer.data;
          BigInt privateKeyVersion = privateKeyDerIn.getInteger();
          BigInt privateKeyModulus = privateKeyDerIn.getInteger();
          BigInt privateKeyExponent = privateKeyDerIn.getInteger();

          if (!publicKeyModulus.equals(privateKeyModulus)) {
              CMS.debug("verifyKeyPair modulus mismatch publicKeyModulus=" + publicKeyModulus + " privateKeyModulus=" + privateKeyModulus);
              return false;
          }

          if (!publicKeyExponent.equals(privateKeyExponent)) {
              CMS.debug("verifyKeyPair exponent mismatch publicKeyExponent=" + publicKeyExponent + " privateKeyExponent=" + privateKeyExponent);
              return false;
          }

          return true;
       } catch (Exception e) {
          CMS.debug("verifyKeyPair error " + e);
          return false;
       }
    }

    private static final OBJECT_IDENTIFIER PKIARCHIVEOPTIONS_OID =
        new OBJECT_IDENTIFIER(new long[] {1, 3, 6, 1, 5, 5, 7, 5, 1, 4}
        );

    /**
     * Retrieves PKIArchiveOptions from CRMF request.
     *
     * @param crmfBlob CRMF request
     * @return PKIArchiveOptions
     * @exception EBaseException failed to extrace option
     */
    public static PKIArchiveOptionsContainer[] getPKIArchiveOptions(String crmfBlob) 
        throws EBaseException {
        Vector options = new Vector();

        if (CMS.debugOn())
            CMS.debug("EnrollmentService::getPKIArchiveOptions> crmfBlob=" + crmfBlob);
        byte[] crmfBerBlob = null;

        crmfBerBlob = com.netscape.osutil.OSUtil.AtoB(crmfBlob);
        ByteArrayInputStream crmfBerBlobIn = new 	
            ByteArrayInputStream(crmfBerBlob);
        SEQUENCE crmfmsgs = null;

        try {
            crmfmsgs = (SEQUENCE) new 
                    SEQUENCE.OF_Template(new 
                        CertReqMsg.Template()).decode(
                        crmfBerBlobIn);
        } catch (IOException e) {
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "[crmf msgs]" + e.toString()));
        } catch (InvalidBERException e) {
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "[crmf msgs]" + e.toString()));
        }

        for (int z = 0; z < crmfmsgs.size(); z++) {
            CertReqMsg certReqMsg = (CertReqMsg)
                crmfmsgs.elementAt(z);
            CertRequest certReq = certReqMsg.getCertReq();	
			
            // try to locate PKIArchiveOption control
            AVA archAva = null;

            try {
                for (int i = 0; i < certReq.numControls(); i++) {
                    AVA ava = certReq.controlAt(i);
                    OBJECT_IDENTIFIER oid = ava.getOID();

                    if (oid.equals(PKIARCHIVEOPTIONS_OID)) {
                        archAva = ava;
                        break;
                    }
                }
            } catch (Exception e) {
                throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "no PKIArchiveOptions found " + e.toString()));
            }
            if (archAva != null) {

                ASN1Value archVal = archAva.getValue();
                ByteArrayInputStream bis = new ByteArrayInputStream(ASN1Util.encode(archVal));
                PKIArchiveOptions archOpts = null;

                try {
                    archOpts = (PKIArchiveOptions)
                            (new PKIArchiveOptions.Template()).decode(bis);
                } catch (IOException e) {
                    throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "[PKIArchiveOptions]" + e.toString()));
                } catch (InvalidBERException e) {
                    throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "[PKIArchiveOptions]" + e.toString()));
                }
                options.addElement(new PKIArchiveOptionsContainer(archOpts, z));
            }
        }
        if (options.size() == 0) {
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "PKIArchiveOptions found"));
        } else {
            PKIArchiveOptionsContainer p[] = new PKIArchiveOptionsContainer[options.size()];	

            options.copyInto(p);
            return p;
        }
    }

    /**
     * Retrieves public key from request.
     *
     * @param request CRMF request
     * @return JSS public key
     * @exception EBaseException failed to retrieve public key
     */
    private X509Key getPublicKey(IRequest request, int i) throws EBaseException {
        String profileId = request.getExtDataInString("profileId");

        if (profileId != null && !profileId.equals("")) {
            byte[] certKeyData = request.getExtDataInByteArray(IEnrollProfile.REQUEST_KEY);
            if (certKeyData != null) {
                try {
                    CertificateX509Key x509key = new CertificateX509Key(
                            new ByteArrayInputStream(certKeyData));

                    return (X509Key) x509key.get(CertificateX509Key.KEY);
                } catch (Exception e1) {
                    CMS.debug("EnrollService: (Archival) getPublicKey " +
                            e1.toString());
                }
            }
            return null;
        }

        // retrieve x509 Key from request
        X509CertInfo certInfo[] =
            request.getExtDataInCertInfoArray(IRequest.CERT_INFO);
        CertificateX509Key pX509Key = null;

        try {
            pX509Key = (CertificateX509Key)
                    certInfo[i].get(X509CertInfo.KEY);
        } catch (IOException e) {
            mKRA.log(ILogger.LL_FAILURE, 
                CMS.getLogMessage("CMSCORE_KRA_GET_PUBLIC_KEY", e.toString()));
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "[" + X509CertInfo.KEY + "]" + e.toString()));
        } catch (CertificateException e) {
            mKRA.log(ILogger.LL_FAILURE, 
                CMS.getLogMessage("CMSCORE_KRA_GET_PUBLIC_KEY", e.toString()));
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "[" + X509CertInfo.KEY + "]" + e.toString()));
        }
        X509Key pKey = null;

        try {
            pKey = (X509Key) pX509Key.get(
                        CertificateX509Key.KEY);
        } catch (IOException e) {
            mKRA.log(ILogger.LL_FAILURE, 
                CMS.getLogMessage("CMSCORE_KRA_GET_PUBLIC_KEY", e.toString()));
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "[" + CertificateX509Key.KEY + "]" + e.toString()));
        }
        return pKey;
    }

    /**
     * Retrieves key's owner name from request.
     *
     * @param request CRMF request
     * @return owner name (subject name)
     * @exception EBaseException failed to retrieve public key
     */
    private String getOwnerName(IRequest request, int i) 
        throws EBaseException {

        String profileId = request.getExtDataInString("profileId");

        if (profileId != null && !profileId.equals("")) {
            CertificateSubjectName sub = request.getExtDataInCertSubjectName(
                    IEnrollProfile.REQUEST_SUBJECT_NAME);
            if (sub != null) {
                return sub.toString();
            }
        }

        X509CertInfo certInfo[] =
            request.getExtDataInCertInfoArray(IRequest.CERT_INFO);
        CertificateSubjectName pSub = null;

        try {
            pSub = (CertificateSubjectName)
                    certInfo[0].get(X509CertInfo.SUBJECT);
        } catch (IOException e) {
            mKRA.log(ILogger.LL_FAILURE, 
                CMS.getLogMessage("CMSCORE_KRA_GET_OWNER_NAME", e.toString()));
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "[" + X509CertInfo.SUBJECT + "]" + e.toString()));
        } catch (CertificateException e) {
            mKRA.log(ILogger.LL_FAILURE, 
                CMS.getLogMessage("CMSCORE_KRA_GET_OWNER_NAME", e.toString()));
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "[" + X509CertInfo.SUBJECT + "]" + e.toString()));
        }
        String owner = pSub.toString();

        return owner;
    }

    /**
     * Signed Audit Log Public Key
     *
     * This method is called to obtain the public key from the passed in
     * "KeyRecord" for a signed audit log message.
     * <P>
     *
     * @param rec a Key Record
     * @return key string containing the certificate's public key
     */
    private String auditPublicKey(KeyRecord rec) {
        // if no signed audit object exists, bail
        if (mSignedAuditLogger == null) {
            return null;
        }

        if (rec == null) {
            return ILogger.SIGNED_AUDIT_EMPTY_VALUE;
        }

        byte rawData[] = null;

        try {
            rawData = rec.getPublicKeyData();
        } catch (EBaseException e) {
            return ILogger.SIGNED_AUDIT_EMPTY_VALUE;
        }

        String key = "";

        // convert "rawData" into "base64Data"
        if (rawData != null) {
            String base64Data = null;

            base64Data = CMS.BtoA(rawData).trim();

            // extract all line separators from the "base64Data"
            StringTokenizer st = new StringTokenizer(base64Data, "\r\n");
            while (st.hasMoreTokens()) {
              key += st.nextToken();
            }
        }

        {
            key = key.trim();

            if (key.equals("")) {
                return ILogger.SIGNED_AUDIT_EMPTY_VALUE;
            } else {
                return key;
            }
        }
    }
    /**
     * Signed Audit Log Subject ID
     *
     * This method is called to obtain the "SubjectID" for
     * a signed audit log message.
     * <P>
     *
     * @return id string containing the signed audit log message SubjectID
     */

     private String auditSubjectID() {
        // if no signed audit object exists, bail
        if (mSignedAuditLogger == null) {
            return null;
        }

        String subjectID = null;

        // Initialize subjectID
        SessionContext auditContext = SessionContext.getExistingContext();

        if (auditContext != null) {
            subjectID = (String)
                    auditContext.get(SessionContext.USER_ID);

            if (subjectID != null) {
                subjectID = subjectID.trim();
            } else {
                subjectID = ILogger.NONROLEUSER;
            }
        } else {
            subjectID = ILogger.UNIDENTIFIED;
        }

        return subjectID;
    }
    /**
     * Signed Audit Log Requester ID
     *
     * This method is called to obtain the "RequesterID" for
     * a signed audit log message.
     * <P>
     *
     * @return id string containing the signed audit log message RequesterID
     */
    private String auditRequesterID() {
        // if no signed audit object exists, bail
        if (mSignedAuditLogger == null) {
            return null;
        }

        String requesterID = null;

        // Initialize requesterID
        SessionContext auditContext = SessionContext.getExistingContext();

        if (auditContext != null) {
            requesterID = (String)
                    auditContext.get(SessionContext.REQUESTER_ID);

            if (requesterID != null) {
                requesterID = requesterID.trim();
            } else {
                requesterID = ILogger.UNIDENTIFIED;
            }
        } else {
            requesterID = ILogger.UNIDENTIFIED;
        }

        return requesterID;
    }

    /**
     * Signed Audit Log Recovery ID
     *
     * This method is called to obtain the "RecoveryID" for
     * a signed audit log message.
     * <P>
     *
     * @return id string containing the signed audit log message RecoveryID
     */
    private String auditRecoveryID() {
        // if no signed audit object exists, bail
        if (mSignedAuditLogger == null) {
            return null;
        }

        String recoveryID = null;

        // Initialize recoveryID
        SessionContext auditContext = SessionContext.getExistingContext();

        if (auditContext != null) {
            recoveryID = (String)
                    auditContext.get(SessionContext.RECOVERY_ID);

            if (recoveryID != null) {
                recoveryID = recoveryID.trim();
            } else {
                recoveryID = ILogger.UNIDENTIFIED;
            }
        } else {
            recoveryID = ILogger.UNIDENTIFIED;
        }

        return recoveryID;
    }

    
    /**
     * Signed Audit Log
     *
     * This method is called to store messages to the signed audit log.
     * <P>
     *
     * @param msg signed audit log message
     */
    private void audit(String msg) {
        // in this case, do NOT strip preceding/trailing whitespace
        // from passed-in String parameters

        if (mSignedAuditLogger == null) {
            return;
        }

        mSignedAuditLogger.log(ILogger.EV_SIGNED_AUDIT,
            null,
            ILogger.S_SIGNED_AUDIT,
            ILogger.LL_SECURITY,
            msg);
    }
}


/**
 * Parsed and Flattened structure of PKIArchiveOptions.
 */
class ArchiveOptions {
    private String mSymmAlgOID = null;
    private byte mSymmAlgParams[] = null;
    private byte mEncSymmKey[] = null;
    private byte mEncValue[] = null;
    public ArchiveOptions(PKIArchiveOptions opts) throws EBaseException {
        try {
            EncryptedKey key = opts.getEncryptedKey();
            ANY enveloped_val = null;
            EncryptedValue val = null;
            AlgorithmIdentifier symmAlg = null;

            if (key.getType() == org.mozilla.jss.pkix.crmf.EncryptedKey.ENVELOPED_DATA) {
                CMS.debug("EnrollService: ArchiveOptions() EncryptedKey type= ENVELOPED_DATA");
                // this is the new RFC4211 EncryptedKey that should
                // have EnvelopedData to replace the deprecated EncryptedValue
                enveloped_val = key.getEnvelopedData();
                byte[] env_b = enveloped_val.getEncoded();
                EnvelopedData.Template env_template = new EnvelopedData.Template();
                EnvelopedData env_data = 
                        (EnvelopedData) env_template.decode(new ByteArrayInputStream(env_b));
                EncryptedContentInfo eCI = env_data.getEncryptedContentInfo();
                symmAlg = eCI.getContentEncryptionAlgorithm();
                mSymmAlgOID = symmAlg.getOID().toString();
                mSymmAlgParams = ((OCTET_STRING) ((ANY) symmAlg.getParameters()).decodeWith(OCTET_STRING.getTemplate())).toByteArray();

                SET recipients = env_data.getRecipientInfos();
                if (recipients.size() <= 0) {
                  CMS.debug("EnrollService: ArchiveOptions() - missing recipient information ");
                  throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "[PKIArchiveOptions] missing recipient information "));
                }
                //check recpient - later
                //we only handle one recipient here anyways.  so, either the key
                //can be decrypted or it can't. No risk here.
                RecipientInfo ri = (RecipientInfo) recipients.elementAt(0);
                OCTET_STRING key_o = ri.getEncryptedKey();
                mEncSymmKey = key_o.toByteArray();

                OCTET_STRING oString = eCI.getEncryptedContent();
                BIT_STRING encVal = new BIT_STRING(oString.toByteArray(), 0);
                mEncValue = encVal.getBits();
                CMS.debug("EnrollService: ArchiveOptions() EncryptedKey type= ENVELOPED_DATA done");
            } else if (key.getType() == org.mozilla.jss.pkix.crmf.EncryptedKey.ENCRYPTED_VALUE) {
                CMS.debug("EnrollService: ArchiveOptions() EncryptedKey type= ENCRYPTED_VALUE");
                // this is deprecated: EncryptedValue
                val = key.getEncryptedValue();
                symmAlg = val.getSymmAlg();
                mSymmAlgOID = symmAlg.getOID().toString();
                mSymmAlgParams = ((OCTET_STRING) ((ANY) symmAlg.getParameters()).decodeWith(OCTET_STRING.getTemplate())).toByteArray();
                BIT_STRING encSymmKey = val.getEncSymmKey();

                mEncSymmKey = encSymmKey.getBits();
                BIT_STRING encVal = val.getEncValue();

                mEncValue = encVal.getBits();
                CMS.debug("EnrollService: ArchiveOptions() EncryptedKey type= ENCRYPTED_VALUE done");
            } else {
                CMS.debug("EnrollService: ArchiveOptions() invalid EncryptedKey type");
                throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "[PKIArchiveOptions] type " + key.getType()));
            }

        } catch (InvalidBERException e) {
            CMS.debug("EnrollService: ArchiveOptions(): " + e.toString());
            throw new EBaseException(CMS.getUserMessage("CMS_BASE_INVALID_ATTRIBUTE", "[PKIArchiveOptions]" + e.toString()));
        } catch (IOException e) {
            CMS.debug("EnrollService: ArchiveOptions(): " + e.toString());
            throw new EBaseException("ArchiveOptions() exception caught: "+
                    e.toString());
        } catch (Exception e) {
            CMS.debug("EnrollService: ArchiveOptions(): " + e.toString());
            throw new EBaseException("ArchiveOptions() exception caught: "+
                    e.toString());
        }

    }

    public String getSymmAlgOID() {
        return mSymmAlgOID;
    }

    public byte[] getSymmAlgParams() {
        return mSymmAlgParams;
    }

    public byte[] getEncSymmKey() {
        return mEncSymmKey;
    }

    public byte[] getEncValue() {
        return mEncValue;
    }
}
