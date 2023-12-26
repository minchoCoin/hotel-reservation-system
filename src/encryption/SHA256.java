package encryption;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @brief 회원가입시나 로그인시, 입력받은 비밀번호를 암호화(SHA256)으로 암호화하는데 사용된다.
 */
public class SHA256 {
	/**
	 * @brief 입력받은 plaintext를 SHA256알고리즘을 이용하여 64글자의 암호문으로 바꾼다.
	 * @param text plaintext
	 * @return 암호화된 text
	 * @throws NoSuchAlgorithmException
	 */
	public String encrypt(String text) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(text.getBytes());

        return bytesToHex(md.digest());
    }
	/**
	 * @brief 바이트를 hex로 바꾼다.
	 * @param bytes 바이트 배열
	 * @return
	 */
	private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }
}
//https://bamdule.tistory.com/233
