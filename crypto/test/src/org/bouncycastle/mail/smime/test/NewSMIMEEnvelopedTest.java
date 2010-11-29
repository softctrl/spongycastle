package org.bouncycastle.mail.smime.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import javax.crypto.Cipher;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cms.CMSAlgorithm;
import org.bouncycastle.cms.RecipientId;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.RecipientInformationStore;
import org.bouncycastle.cms.jcajce.JceCMSContentEncryptorBuilder;
import org.bouncycastle.cms.jcajce.JceKeyTransEnvelopedRecipient;
import org.bouncycastle.cms.jcajce.JceKeyTransRecipientInfoGenerator;
import org.bouncycastle.cms.test.CMSTestUtil;
import org.bouncycastle.jce.PrincipalUtil;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMEEnvelopedGenerator;
import org.bouncycastle.mail.smime.SMIMEEnvelopedParser;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.mail.smime.util.FileBackedMimeBodyPart;

public class NewSMIMEEnvelopedTest 
    extends TestCase
{
    private static final String BC = BouncyCastleProvider.PROVIDER_NAME;

    private static String          _signDN;
    private static KeyPair         _signKP;

    private static String          _reciDN;
    private static KeyPair         _reciKP;
    private static X509Certificate _reciCert;

    private static String          _reciDN2;
    private static KeyPair         _reciKP2;
    private static X509Certificate _reciCert2;

    private static boolean         _initialised = false;

    private static void init()
        throws Exception
    {
        if (!_initialised)
        {
            _initialised = true;

            _signDN   = "O=Bouncy Castle, C=AU";
            _signKP   = CMSTestUtil.makeKeyPair();

            _reciDN   = "CN=Doug, OU=Sales, O=Bouncy Castle, C=AU";
            _reciKP   = CMSTestUtil.makeKeyPair();
            _reciCert = CMSTestUtil.makeCertificate(_reciKP, _reciDN, _signKP, _signDN);

            _reciDN2   = "CN=Fred, OU=Sales, O=Bouncy Castle, C=AU";
            _reciKP2   = CMSTestUtil.makeKeyPair();
            _reciCert2 = CMSTestUtil.makeCertificate(_reciKP2, _reciDN2, _signKP, _signDN);
        }
    }

    public NewSMIMEEnvelopedTest(
        String name) 
    {
        super(name);
    }

    public static void main(
        String args[]) 
    {
        junit.textui.TestRunner.run(NewSMIMEEnvelopedTest.class);
    }

    public static Test suite() 
        throws Exception 
    {
        return new SMIMETestSetup(new TestSuite(NewSMIMEEnvelopedTest.class));
    }

    public void setUp()
        throws Exception
    {
        init();
    }
    
    public void testHeaders()
        throws Exception
    {
        MimeBodyPart    msg      = SMIMETestUtil.makeMimeBodyPart("WallaWallaWashington");

        SMIMEEnvelopedGenerator  gen = new SMIMEEnvelopedGenerator();
          
        gen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(_reciCert).setProvider(BC));
         
        //
        // generate a MimeBodyPart object which encapsulates the content
        // we want encrypted.
        //

        MimeBodyPart mp = gen.generate(msg, new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC).setProvider(BC).build());

        assertEquals("application/pkcs7-mime; name=\"smime.p7m\"; smime-type=enveloped-data", mp.getHeader("Content-Type")[0]);
        assertEquals("attachment; filename=\"smime.p7m\"", mp.getHeader("Content-Disposition")[0]);
        assertEquals("S/MIME Encrypted Message", mp.getHeader("Content-Description")[0]);
    }
    
    public void testDESEDE3Encrypted()
        throws Exception
    {
        MimeBodyPart  msg      = SMIMETestUtil.makeMimeBodyPart("WallaWallaWashington");
        String        algorithm = SMIMEEnvelopedGenerator.DES_EDE3_CBC;
        
        verifyAlgorithm(algorithm, msg);
    }

    public void testParserDESEDE3Encrypted()
        throws Exception
    {
        MimeBodyPart  msg      = SMIMETestUtil.makeMimeBodyPart("WallaWallaWashington");
        String        algorithm = SMIMEEnvelopedGenerator.DES_EDE3_CBC;
        
        verifyParserAlgorithm(algorithm, msg);
    }
    
    public void testIDEAEncrypted()
        throws Exception
    {
        if (isPresent("IDEA"))
        {
            MimeBodyPart  msg      = SMIMETestUtil.makeMimeBodyPart("WallaWallaWashington");
            String        algorithm = SMIMEEnvelopedGenerator.IDEA_CBC;

            verifyAlgorithm(algorithm, msg);
        }
    }

    private boolean isPresent(String algorithm)
        throws Exception
    {
        try
        {
            Cipher.getInstance(algorithm, BC);

            return true;
        }
        catch (NoSuchAlgorithmException e)
        {
            return false;
        }
    }

    public void testRC2Encrypted()
        throws Exception
    {
        MimeBodyPart  msg      = SMIMETestUtil.makeMimeBodyPart("WallaWallaWashington");
        String        algorithm = SMIMEEnvelopedGenerator.RC2_CBC;
        
        verifyAlgorithm(algorithm, msg);
    }

    public void testCASTEncrypted()
        throws Exception
    {
        MimeBodyPart  msg      = SMIMETestUtil.makeMimeBodyPart("WallaWallaWashington");
        String        algorithm = SMIMEEnvelopedGenerator.CAST5_CBC;
        
        verifyAlgorithm(algorithm, msg);
    }
    
    public void testAES128Encrypted()
        throws Exception
    {
        MimeBodyPart  msg      = SMIMETestUtil.makeMimeBodyPart("WallaWallaWashington");
        String        algorithm = SMIMEEnvelopedGenerator.AES128_CBC;
        
        verifyAlgorithm(algorithm, msg);
    }
    
    public void testAES192Encrypted()
        throws Exception
    {
        MimeBodyPart  msg      = SMIMETestUtil.makeMimeBodyPart("WallaWallaWashington");
        String        algorithm = SMIMEEnvelopedGenerator.AES192_CBC;
        
        verifyAlgorithm(algorithm, msg);
    }
    
    public void testAES256Encrypted()
        throws Exception
    {
        MimeBodyPart  msg      = SMIMETestUtil.makeMimeBodyPart("WallaWallaWashington");
        String        algorithm = SMIMEEnvelopedGenerator.AES256_CBC;
        
        verifyAlgorithm(algorithm, msg);
    }
    
    public void testSubKeyId()
        throws Exception
    {
        MimeBodyPart    msg      = SMIMETestUtil.makeMimeBodyPart("WallaWallaWashington");

        SMIMEEnvelopedGenerator   gen = new SMIMEEnvelopedGenerator();

        //
        // create a subject key id - this has to be done the same way as
        // it is done in the certificate associated with the private key
        //
        MessageDigest           dig = MessageDigest.getInstance("SHA1", BC);
        dig.update(SubjectPublicKeyInfo.getInstance(_reciCert.getPublicKey().getEncoded()).getPublicKeyData().getBytes());

          
        gen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(dig.digest(), _reciCert.getPublicKey()).setProvider(BC));
         
        //
        // generate a MimeBodyPart object which encapsulates the content
        // we want encrypted.
        //

        MimeBodyPart         mp = gen.generate(msg, new JceCMSContentEncryptorBuilder(CMSAlgorithm.DES_EDE3_CBC).setProvider(BC).build());

        SMIMEEnveloped       m = new SMIMEEnveloped(mp);

        RecipientId          recId = new RecipientId();

        dig.update(SubjectPublicKeyInfo.getInstance(_reciCert.getPublicKey().getEncoded()).getPublicKeyData().getBytes());
       
        recId.setSubjectKeyIdentifier(dig.digest());

        RecipientInformationStore  recipients = m.getRecipientInfos();
        RecipientInformation       recipient = recipients.get(recId);

        MimeBodyPart    res = SMIMEUtil.toMimeBodyPart(recipient.getContent(new JceKeyTransEnvelopedRecipient(_reciKP.getPrivate()).setProvider(BC)));

        verifyMessageBytes(msg, res);
    }

    public void testCapEncrypt()
        throws Exception
    {
        MimeBodyPart    msg      = SMIMETestUtil.makeMimeBodyPart("WallaWallaWashington");

        SMIMEEnvelopedGenerator   gen = new SMIMEEnvelopedGenerator();

        //
        // create a subject key id - this has to be done the same way as
        // it is done in the certificate associated with the private key
        //
        MessageDigest           dig = MessageDigest.getInstance("SHA1", BC);

        dig.update(_reciCert.getPublicKey().getEncoded());

        gen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(dig.digest(), _reciCert.getPublicKey()));
         
        //
        // generate a MimeBodyPart object which encapsulates the content
        // we want encrypted.
        //
        MimeBodyPart mp = gen.generate(msg, new JceCMSContentEncryptorBuilder(CMSAlgorithm.RC2_CBC, 40).setProvider(BC).build());

        SMIMEEnveloped       m = new SMIMEEnveloped(mp);

        RecipientId          recId = new RecipientId();

        dig.update(_reciCert.getPublicKey().getEncoded());

        recId.setSubjectKeyIdentifier(dig.digest());

        RecipientInformationStore  recipients = m.getRecipientInfos();
        RecipientInformation       recipient = recipients.get(recId);

        MimeBodyPart    res = SMIMEUtil.toMimeBodyPart(recipient.getContent(new JceKeyTransEnvelopedRecipient(_reciKP.getPrivate()).setProvider(BC)));

        verifyMessageBytes(msg, res);
    }
    
    public void testTwoRecipients()
        throws Exception
    {
        MimeBodyPart    _msg      = SMIMETestUtil.makeMimeBodyPart("WallaWallaWashington");

        SMIMEEnvelopedGenerator   gen = new SMIMEEnvelopedGenerator();

        gen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(_reciCert).setProvider(BC));
        gen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(_reciCert2).setProvider(BC));
         
        //
        // generate a MimeBodyPart object which encapsulates the content
        // we want encrypted.
        //
        MimeBodyPart mp = gen.generate(_msg, new JceCMSContentEncryptorBuilder(CMSAlgorithm.RC2_CBC, 40).setProvider(BC).build());

        SMIMEEnvelopedParser       m = new SMIMEEnvelopedParser(mp);

        RecipientId                recId = getRecipientId(_reciCert2);

        RecipientInformationStore  recipients = m.getRecipientInfos();
        RecipientInformation       recipient = recipients.get(recId);
        
        FileBackedMimeBodyPart    res = SMIMEUtil.toMimeBodyPart(recipient.getContentStream(new JceKeyTransEnvelopedRecipient(_reciKP2.getPrivate()).setProvider(BC)));

        verifyMessageBytes(_msg, res);
        
        m = new SMIMEEnvelopedParser(mp);

        res.dispose();
        
        recId = getRecipientId(_reciCert);

        recipients = m.getRecipientInfos();
        recipient = recipients.get(recId);
 
        res = SMIMEUtil.toMimeBodyPart(recipient.getContentStream(new JceKeyTransEnvelopedRecipient(_reciKP.getPrivate()).setProvider(BC)));

        verifyMessageBytes(_msg, res);
        
        res.dispose();
    }
    
    private void verifyAlgorithm(
        String algorithmOid,
        MimeBodyPart msg) 
        throws Exception
    {
        SMIMEEnvelopedGenerator  gen = new SMIMEEnvelopedGenerator();
          
        gen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(_reciCert).setProvider(BC));
         
        //
        // generate a MimeBodyPart object which encapsulates the content
        // we want encrypted.
        //

        MimeBodyPart   mp = gen.generate(msg, new JceCMSContentEncryptorBuilder(new ASN1ObjectIdentifier(algorithmOid)).setProvider(BC).build());
        SMIMEEnveloped m = new SMIMEEnveloped(mp);
        RecipientId    recId = getRecipientId(_reciCert);

        RecipientInformationStore  recipients = m.getRecipientInfos();
        RecipientInformation       recipient = recipients.get(recId);

        MimeBodyPart    res = SMIMEUtil.toMimeBodyPart(recipient.getContent(new JceKeyTransEnvelopedRecipient(_reciKP.getPrivate()).setProvider(BC)));

        verifyMessageBytes(msg, res);
    }
    
    private void verifyParserAlgorithm(
        String algorithmOid,
        MimeBodyPart msg) 
        throws Exception
    {
        SMIMEEnvelopedGenerator  gen = new SMIMEEnvelopedGenerator();

        gen.addRecipientInfoGenerator(new JceKeyTransRecipientInfoGenerator(_reciCert).setProvider(BC));

        //
        // generate a MimeBodyPart object which encapsulates the content
        // we want encrypted.
        //

        MimeBodyPart         mp = gen.generate(msg, new JceCMSContentEncryptorBuilder(new ASN1ObjectIdentifier(algorithmOid)).setProvider(BC).build());
        SMIMEEnvelopedParser m = new SMIMEEnvelopedParser(mp);
        RecipientId          recId = getRecipientId(_reciCert);

        RecipientInformationStore  recipients = m.getRecipientInfos();
        RecipientInformation       recipient = recipients.get(recId);

        MimeBodyPart    res = SMIMEUtil.toMimeBodyPart(recipient.getContent(new JceKeyTransEnvelopedRecipient(_reciKP.getPrivate()).setProvider(BC)));

        verifyMessageBytes(msg, res);
    }

    private RecipientId getRecipientId(
        X509Certificate cert) 
        throws IOException, CertificateEncodingException
    {
        RecipientId          recId = new RecipientId();

        recId.setSerialNumber(cert.getSerialNumber());
        recId.setIssuer(PrincipalUtil.getIssuerX509Principal(cert).getEncoded());
        return recId;
    }
    
    
    private void verifyMessageBytes(MimeBodyPart a, MimeBodyPart b) 
        throws IOException, MessagingException
    {
        ByteArrayOutputStream _baos = new ByteArrayOutputStream();
        a.writeTo(_baos);
        _baos.close();
        byte[] _msgBytes = _baos.toByteArray();
        _baos = new ByteArrayOutputStream();
        b.writeTo(_baos);
        _baos.close();
        byte[] _resBytes = _baos.toByteArray();
        
        assertEquals(true, Arrays.equals(_msgBytes, _resBytes));
    }
}
