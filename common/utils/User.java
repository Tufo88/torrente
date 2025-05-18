package utils;

import java.io.Serializable;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Objects;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class User implements Serializable {

	private static final long serialVersionUID = 1L;

	private final String _id;
	private InetAddress _ip;

	private String _hash; // hashed password
	private String _salt;

	private static final String ENCRYPTION_ALGORITHM = "PBKDF2WithHmacSHA1";

	public User(String id, InetAddress ip, String hash, String salt) {

		_id = id;
		_ip = ip;
		_hash = hash;
		_salt = salt;

	}

	public boolean checkPassword(char[] pass) {
		Base64.Decoder dec = Base64.getDecoder();
		if (this._salt == null)
			return false;

		byte[] salt = dec.decode(this._salt);

		KeySpec spec = new PBEKeySpec(pass, salt, 65536, 128);
		SecretKeyFactory f;
		try {
			f = SecretKeyFactory.getInstance(ENCRYPTION_ALGORITHM);
			byte[] hash = f.generateSecret(spec).getEncoded();
			Base64.Encoder enc = Base64.getEncoder();
			String _pass = enc.encodeToString(hash);

			return _pass.equals(this._hash);

		} catch (Exception e1) {
			e1.printStackTrace();
		}
		return false;
	}

	public static Pair<String, String> generateCypheredPass(char[] pass) {
		SecureRandom random = new SecureRandom();
		byte[] salt = new byte[16];
		random.nextBytes(salt);
		KeySpec spec = new PBEKeySpec(pass, salt, 65536, 128);
		SecretKeyFactory f;
		byte[] hash;
		try {
			f = SecretKeyFactory.getInstance(ENCRYPTION_ALGORITHM);
			hash = f.generateSecret(spec).getEncoded();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}

		Base64.Encoder enc = Base64.getEncoder();
		return new Pair<>(enc.encodeToString(hash), enc.encodeToString(salt));
	}

	public void updateIp(InetAddress newIp) {

		_ip = newIp;
	}

	public InetAddress get_ip() {
		return _ip;
	}

	public String get_hash() {
		return _hash;
	}

	public String get_salt() {
		return _salt;
	}

	public String get_id() {
		return _id;
	}

	@Override
	public int hashCode() {
		return Objects.hash(_hash, _id, _ip, _salt);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		User other = (User) obj;
		return Objects.equals(_hash, other._hash) && Objects.equals(_id, other._id) && Objects.equals(_ip, other._ip)
				&& Objects.equals(_salt, other._salt);
	}

}
