package hellojsp.util;

import java.io.Writer;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class AES {
	
	private byte[] key = {'h','e','l','l','o','j','s','p','i','s','s','o','g','o','o','d'};
	private byte[] iv = new byte[16];

	private boolean debug = false;
	private Writer out;
	
	public AES() {}
	
	public AES(String key) {
		setKey(key);
	}

	public AES(String key, String iv) {
		setKey(key, iv);
	}
	
	public void setDebug(Writer out) {
		this.debug = true;
		this.out = out;
	}
	
	public void setDebug() {
		this.debug = true;
		this.out = null;
	}
	
	public void setError(String msg, Exception ex) {
		try {
			if(null != out && debug == true) out.write("<hr>" + msg + "###" + ex + "<hr>");
			if(ex != null || debug == true) Hello.errorLog(msg, ex);
		} catch(Exception e) {}
	}
	
	public void setKey(String keyString) {
		setKey(keyString, null);
	}
	
	public void setKey(String keyString, String ivString) {
		try {
			this.key = keyString.getBytes("UTF-8");
			this.iv = ivString != null ? ivString.getBytes("UTF-8") : new byte[16];
			if(key.length != 16 && key.length != 24 && key.length != 32) {
				setError("", new Exception("Key is not 16 bytes or 24 bytes or 32 bytes"));
			}
			if(iv.length != 16) {
				setError("", new Exception("IV is not 16 bytes"));
			}
		} catch(Exception e) {
			setError("{AES.setKey} key:" + key, e);
		}
	}

	public String encrypt(String value) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(iv);
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
			byte[] encValue = cipher.doFinal(value.getBytes("UTF-8"));
			return new String(Base64Coder.enc(encValue));
		} catch(Exception e) {
			setError("{AES.encrypt} value:" + value, e);
		}
		return "";
	}

	public String decrypt(String value) {
		try {
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
			IvParameterSpec ivSpec = new IvParameterSpec(iv);
			cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
			byte[] decValue = cipher.doFinal(Base64Coder.dec(value));
			return new String(decValue, "UTF-8");
		} catch(Exception e) {
			setError("{AES.decrypt} value:" + value, e);
		}
		return value;
	}
}