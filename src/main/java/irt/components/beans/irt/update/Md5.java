package irt.components.beans.irt.update;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.xml.bind.DatatypeConverter;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor @Getter
public class Md5 {

	@NonNull private final byte[] bytes;

	public String toString() {
		try {

			return DatatypeConverter.printHexBinary(MessageDigest.getInstance("MD5").digest(bytes));

		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}
}
