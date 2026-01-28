package mg.eqima.jiramacashless.encrypt;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 *
 * @author Tino Ran
 */
public class Encrypt {
    public  String SHA256(String text)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        return hashAlgorithm("SHA-256", text);
    }

    /**
     * Converts a String into a specified hash.
     *
     * @param text
     *            Text to convert.
     * @return hash.
     * @throws NoSuchAlgorithmException
     * @throws UnsupportedEncodingException
     */
    public String hashAlgorithm(String hash, String text)
            throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //TapjoyLog.i(TAPJOY_UTIL, "" + hash + ": " + text);

        MessageDigest md;
        byte[] sha1hash = new byte[40];

        // MD5, SHA-1, etc
        md = MessageDigest.getInstance(hash);
        md.update(text.getBytes("iso-8859-1"), 0, text.length());
        sha1hash = md.digest();

        return convertToHex(sha1hash);
    }

    /**
     * Converts a byte array into a hex string.
     *
     * @param data
     *            Data to convert.
     * @return Data in hex as a string.
     */
    private String convertToHex(byte[] data) {
        StringBuffer buf = new StringBuffer();

        for (int i = 0; i < data.length; i++) {
            int halfbyte = (data[i] >>> 4) & 0x0F;
            int two_halfs = 0;

            do {
                if ((0 <= halfbyte) && (halfbyte <= 9))
                    buf.append((char) ('0' + halfbyte));
                else
                    buf.append((char) ('a' + (halfbyte - 10)));
                halfbyte = data[i] & 0x0F;
            }

            while (two_halfs++ < 1);
        }

        return buf.toString();
    }
}

