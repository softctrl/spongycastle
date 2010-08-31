package org.bouncycastle.cert.crmf;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.security.SecureRandom;

import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.cmp.CMPObjectIdentifiers;
import org.bouncycastle.asn1.cmp.PBMParameter;
import org.bouncycastle.asn1.iana.IANAObjectIdentifiers;
import org.bouncycastle.asn1.oiw.OIWObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.operator.MacCalculator;
import org.bouncycastle.operator.RuntimeOperatorException;
import org.bouncycastle.util.Strings;

public class PKMACBuilder
{
    private AlgorithmIdentifier owf;
    private int iterationCount;
    private AlgorithmIdentifier mac;
    private int saltLength = 20;
    private SecureRandom random;
    private PKMACValuesCalculator calculator;

    public PKMACBuilder(PKMACValuesCalculator calculator)
    {
        this(new AlgorithmIdentifier(OIWObjectIdentifiers.idSHA1), 1000, new AlgorithmIdentifier(IANAObjectIdentifiers.hmacSHA1, DERNull.INSTANCE), calculator);
    }

    private PKMACBuilder(AlgorithmIdentifier hashAlgorithm, int iterationCount, AlgorithmIdentifier macAlgorithm, PKMACValuesCalculator calculator)
    {
        if (iterationCount < 100)
        {
            throw new IllegalArgumentException("iteration count must be at least 100");
        }

        this.owf = hashAlgorithm;
        this.iterationCount = iterationCount;
        this.mac = macAlgorithm;
        this.calculator = calculator;
    }

    /**
     * Set the salt length in octets.
     *
     * @param saltLength length in octets of the salt to be generated.
     * @return the generator
     */
    public PKMACBuilder setSaltLength(int saltLength)
    {
        if (saltLength < 8)
        {
            throw new IllegalArgumentException("salt length must be at least 8 bytes");
        }

        this.saltLength = saltLength;

        return this;
    }

    public PKMACBuilder setIterationCount(int iterationCount)
    {
        this.iterationCount = iterationCount;

        return this;
    }

    public PKMACBuilder setRandom(SecureRandom random)
    {
        this.random = random;

        return this;
    }

    public MacCalculator build(PBMParameter parameters, char[] password)
        throws CRMFException
    {
        calculator.setup(parameters.getOwf(), parameters.getMac());

        return genCalculator(parameters.getSalt().getOctets(), parameters.getIterationCount().getValue().intValue(), password);
    }

    public MacCalculator build(char[] password)
        throws CRMFException
    {
        // From RFC 4211
        //
        //   1.  Generate a random salt value S
        //
        //   2.  Append the salt to the pw.  K = pw || salt.
        //
        //   3.  Hash the value of K.  K = HASH(K)
        //
        //   4.  If Iter is greater than zero.  Iter = Iter - 1.  Goto step 3.
        //
        //   5.  Compute an HMAC as documented in [HMAC].
        //
        //       MAC = HASH( K XOR opad, HASH( K XOR ipad, data) )
        //
        //       Where opad and ipad are defined in [HMAC].

        final byte[] salt = new byte[saltLength];

        if (random == null)
        {
            this.random = new SecureRandom();
        }

        random.nextBytes(salt);

        calculator.setup(owf, mac);

        return genCalculator(salt, iterationCount, password);
    }

    private MacCalculator genCalculator(final byte[] salt, int itCount, char[] password)
        throws CRMFException
    {
        byte[] pw = Strings.toUTF8ByteArray(password);
        byte[] K = new byte[pw.length + salt.length];

        System.arraycopy(pw, 0, K, 0, pw.length);
        System.arraycopy(salt, 0, K, pw.length, salt.length);

        int iter = itCount;
        do
        {
            K = calculator.calculateDigest(K);
        }
        while (--iter > 0);

        final byte[] key = K;

        return new MacCalculator()
        {
            ByteArrayOutputStream bOut = new ByteArrayOutputStream();

            public AlgorithmIdentifier getAlgorithmIdentifier()
            {
                return new AlgorithmIdentifier(CMPObjectIdentifiers.passwordBasedMac, new PBMParameter(salt, owf, iterationCount, mac));
            }

            public OutputStream getOutputStream()
            {
                return bOut;
            }

            public byte[] getMac()
            {
                try
                {
                    return calculator.calculateMac(key, bOut.toByteArray());
                }
                catch (CRMFException e)
                {
                    throw new RuntimeOperatorException("exception calculating mac: " + e.getMessage(), e);
                }
            }
        };
    }
}
