/*
 * matrix-client
 * Copyright (C) 2017 Julius Lehmann
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */

package org.matrix.olm;

import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Logger;

import com.beust.klaxon.JsonObject;

/**
 * Olm SDK helper class.
 */
public class OlmUtility
{
	private static final Logger LOGGER = Logger.getLogger(OlmUtility.class.getName());
	
	public static final int RANDOM_KEY_SIZE = 32;
	
	/**
	 * Instance Id returned by JNI.
	 * This value uniquely identifies this utility instance.
	 **/
	private long mNativeId;
	
	public OlmUtility()
			throws OlmException
	{
		initUtility();
	}
	
	/**
	 * Create a native utility instance.
	 * To be called before any other API call.
	 *
	 * @throws OlmException the exception
	 */
	private void initUtility()
			throws OlmException
	{
		try
		{
			mNativeId = createUtilityJni();
		}
		catch (Exception e)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_UTILITY_CREATION, e.getMessage());
		}
	}
	
	private native long createUtilityJni();
	
	/**
	 * Release native instance.<br>
	 * Public API for {@link #releaseUtilityJni()}.
	 */
	public void releaseUtility()
	{
		if (0 != mNativeId)
		{
			releaseUtilityJni();
		}
		mNativeId = 0;
	}
	
	private native void releaseUtilityJni();
	
	/**
	 * Verify an ed25519 signature.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aSignature      the base64-encoded message signature to be checked.
	 * @param aFingerprintKey the ed25519 key (fingerprint key)
	 * @param aMessage        the signed message
	 * @throws OlmException the failure reason
	 */
	public void verifyEd25519Signature(String aSignature, String aFingerprintKey, String aMessage)
			throws OlmException
	{
		String errorMessage;
		
		try
		{
			if (aSignature.isEmpty() || aFingerprintKey.isEmpty() || aMessage.isEmpty())
			{
				LOGGER.severe("## verifyEd25519Signature(): invalid input parameters");
				errorMessage = "JAVA sanity check failure - invalid input parameters";
			}
			else
			{
				errorMessage = verifyEd25519SignatureJni(aSignature.getBytes("UTF-8"), aFingerprintKey.getBytes("UTF-8"), aMessage.getBytes("UTF-8"));
			}
		}
		catch (Exception e)
		{
			errorMessage = e.getMessage();
			LOGGER.severe("## verifyEd25519Signature(): failed " + errorMessage);
		}
		
		if (errorMessage != null)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_UTILITY_VERIFY_SIGNATURE, errorMessage);
		}
	}
	
	/**
	 * Verify an ed25519 signature.
	 * Return a human readable error message in case of verification failure.
	 *
	 * @param aSignature      the base64-encoded message signature to be checked.
	 * @param aFingerprintKey the ed25519 key
	 * @param aMessage        the signed message
	 * @return null if validation succeed, the error message string if operation failed
	 */
	private native String verifyEd25519SignatureJni(byte[] aSignature, byte[] aFingerprintKey, byte[] aMessage);
	
	/**
	 * Compute the hash(SHA-256) value of the string given in parameter(aMessageToHash).<br>
	 * The hash value is the returned by the method.
	 *
	 * @param aMessageToHash message to be hashed
	 * @return hash value if operation succeed, null otherwise
	 */
	public String sha256(String aMessageToHash)
	{
		String hashRetValue = null;
		
		if (null != aMessageToHash)
		{
			try
			{
				hashRetValue = new String(sha256Jni(aMessageToHash.getBytes("UTF-8")), "UTF-8");
			}
			catch (Exception e)
			{
				LOGGER.severe("## sha256(): failed " + e.getMessage());
			}
		}
		
		return hashRetValue;
	}
	
	/**
	 * Compute the digest (SHA 256) for the message passed in parameter.<br>
	 * The digest value is the function return value.
	 * An exception is thrown if the operation fails.
	 *
	 * @param aMessage the message
	 * @return digest of the message.
	 **/
	private native byte[] sha256Jni(byte[] aMessage);
	
	/**
	 * Helper method to compute a string based on random integers.
	 *
	 * @return bytes buffer containing randoms integer values
	 */
	public static byte[] getRandomKey()
	{
		SecureRandom secureRandom = new SecureRandom();
		byte[] buffer = new byte[RANDOM_KEY_SIZE];
		secureRandom.nextBytes(buffer);
		
		// the key is saved as string
		// so avoid the UTF8 marker bytes
		for (int i = 0; i < RANDOM_KEY_SIZE; i++)
		{
			buffer[i] = (byte) (buffer[i] & 0x7F);
		}
		return buffer;
	}
	
	/**
	 * Return true the object resources have been released.<br>
	 *
	 * @return true the object resources have been released
	 */
	public boolean isReleased()
	{
		return (0 == mNativeId);
	}
	
	/**
	 * Build a string-string dictionary from a jsonObject.<br>
	 *
	 * @param jsonObject the object to parse
	 * @return the map
	 */
	public static Map<String, String> toStringMap(JsonObject jsonObject)
	{
		if (null != jsonObject)
		{
			HashMap<String, String> map = new HashMap<>();
			for (String key : jsonObject.getKeys())
			{
				try
				{
					Object value = jsonObject.get(key);
					
					if (value instanceof String)
					{
						map.put(key, (String) value);
					}
					else
					{
						LOGGER.severe("## toStringMap(): unexpected type " + value.getClass());
					}
				}
				catch (Exception e)
				{
					LOGGER.severe("## toStringMap(): failed " + e.getMessage());
				}
			}
			
			return map;
		}
		
		return null;
	}
	
	/**
	 * Build a string-string dictionary of string dictionary from a jsonObject.<br>
	 *
	 * @param jsonObject the object to parse
	 * @return the map
	 */
	public static Map<String, Map<String, String>> toStringMapMap(JsonObject jsonObject)
	{
		if (null != jsonObject)
		{
			HashMap<String, Map<String, String>> map = new HashMap<>();
			
			for (String key : jsonObject.getKeys())
			{
				try
				{
					Object value = jsonObject.get(key);
					
					if (value instanceof JsonObject)
					{
						map.put(key, toStringMap((JsonObject) value));
					}
					else
					{
						LOGGER.severe("## toStringMapMap(): unexpected type " + value.getClass());
					}
				}
				catch (Exception e)
				{
					LOGGER.severe("## toStringMapMap(): failed " + e.getMessage());
				}
			}
			
			return map;
		}
		
		return null;
	}
}

