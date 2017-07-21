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

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

import com.beust.klaxon.*;


/**
 * Account class used to create Olm sessions in conjunction with {@link OlmSession} class.<br>
 * OlmAccount provides APIs to retrieve the Olm keys.
 * <br><br>Detailed implementation guide is available at <a href="http://matrix.org/docs/guides/e2e_implementation.html">Implementing End-to-End Encryption in Matrix clients</a>.
 */
public class OlmAccount extends CommonSerializeUtils implements Serializable
{
	private static final long serialVersionUID = 3497486121598434824L;
	private static final Logger LOGGER = Logger.getLogger(OlmAccount.class.getName());
	
	// JSON keys used in the JSON objects returned by JNI
	/**
	 * As well as the identity key, each device creates a number of Curve25519 key pairs which are
	 * also used to establish Olm sessions, but can only be used once. Once again, the private part
	 * remains on the device. but the public part is published to the Matrix network
	 **/
	public static final String JSON_KEY_ONE_TIME_KEY = "curve25519";
	
	/**
	 * Curve25519 identity key is a public-key cryptographic system which can be used to establish a shared
	 * secret.<br>In Matrix, each device has a long-lived Curve25519 identity key which is used to establish
	 * Olm sessions with that device. The private key should never leave the device, but the
	 * public part is signed with the Ed25519 fingerprint key ({@link #JSON_KEY_FINGER_PRINT_KEY}) and published to the network.
	 **/
	public static final String JSON_KEY_IDENTITY_KEY = "curve25519";
	
	/**
	 * Ed25519 finger print is a public-key cryptographic system for signing messages.<br>In Matrix, each device has
	 * an Ed25519 key pair which serves to identify that device. The private the key should
	 * never leave the device, but the public part is published to the Matrix network.
	 **/
	public static final String JSON_KEY_FINGER_PRINT_KEY = "ed25519";
	
	/**
	 * Account Id returned by JNI.
	 * This value identifies uniquely the native account instance.
	 */
	private transient long mNativeId;
	
	public OlmAccount()
			throws OlmException
	{
		try
		{
			mNativeId = createNewAccountJni();
		}
		catch (Exception e)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_INIT_ACCOUNT_CREATION, e.getMessage());
		}
	}
	
	/**
	 * Create a new account and return it to JAVA side.<br>
	 * Since a C prt is returned as a jlong, special care will be taken
	 * to make the cast (OlmAccount* to jlong) platform independent.
	 *
	 * @return the initialized OlmAccount* instance or throw an exception if fails
	 **/
	private native long createNewAccountJni();
	
	/**
	 * Getter on the account ID.
	 *
	 * @return native account ID
	 */
	long getOlmAccountId()
	{
		return mNativeId;
	}
	
	/**
	 * Release native account and invalid its JAVA reference counter part.<br>
	 * Public API for {@link #releaseAccountJni()}.
	 */
	public void releaseAccount()
	{
		if (0 != mNativeId)
		{
			releaseAccountJni();
		}
		mNativeId = 0;
	}
	
	/**
	 * Destroy the corresponding OLM account native object.<br>
	 * This method must ALWAYS be called when this JAVA instance
	 * is destroyed (ie. garbage collected) to prevent memory leak in native side.
	 * See {@link #createNewAccountJni()}.
	 */
	private native void releaseAccountJni();
	
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
	 * Return the identity keys (identity and fingerprint keys) in a dictionary.<br>
	 * Public API for {@link #identityKeysJni()}.<br>
	 * Ex:<tt>
	 * {
	 * "curve25519":"Vam++zZPMqDQM6ANKpO/uAl5ViJSHxV9hd+b0/fwRAg",
	 * "ed25519":"+v8SOlOASFTMrX3MCKBM4iVnYoZ+JIjpNt1fi8Z9O2I"
	 * }</tt>
	 *
	 * @return identity keys dictionary if operation succeeds, null otherwise
	 * @throws OlmException the failure reason
	 */
	public Map<String, String> identityKeys()
			throws OlmException
	{
		JsonObject identityKeysJsonObj = null;
		
		byte[] identityKeysBuffer;
		
		try
		{
			identityKeysBuffer = identityKeysJni();
		}
		catch (Exception e)
		{
			LOGGER.severe("## identityKeys(): Failure - " + e.getMessage());
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_IDENTITY_KEYS, e.getMessage());
		}
		
		if (null != identityKeysBuffer)
		{
			try
			{
				
				identityKeysJsonObj = (JsonObject) new Parser().parse(String.valueOf(identityKeysBuffer));
			}
			catch (Exception e)
			{
				LOGGER.severe("## identityKeys(): Exception - Msg=" + e.getMessage());
			}
		}
		else
		{
			LOGGER.severe("## identityKeys(): Failure - identityKeysJni()=null");
		}
		
		return OlmUtility.toStringMap(identityKeysJsonObj);
	}
	
	/**
	 * Get the public identity keys (Ed25519 fingerprint key and Curve25519 identity key).<br>
	 * Keys are Base64 encoded.
	 * These keys must be published on the server.
	 *
	 * @return the identity keys or throw an exception if it fails
	 */
	private native byte[] identityKeysJni();
	
	/**
	 * Return the largest number of "one time keys" this account can store.
	 *
	 * @return the max number of "one time keys", -1 otherwise
	 */
	public long maxOneTimeKeys()
	{
		return maxOneTimeKeysJni();
	}
	
	/**
	 * Return the largest number of "one time keys" this account can store.
	 *
	 * @return the max number of "one time keys", -1 otherwise
	 */
	private native long maxOneTimeKeysJni();
	
	/**
	 * Generate a number of new one time keys.<br> If total number of keys stored
	 * by this account exceeds {@link #maxOneTimeKeys()}, the old keys are discarded.<br>
	 * The corresponding keys are retrieved by {@link #oneTimeKeys()}.
	 *
	 * @param aNumberOfKeys number of keys to generate
	 * @throws OlmException the failure reason
	 */
	public void generateOneTimeKeys(int aNumberOfKeys)
			throws OlmException
	{
		try
		{
			generateOneTimeKeysJni(aNumberOfKeys);
		}
		catch (Exception e)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_GENERATE_ONE_TIME_KEYS, e.getMessage());
		}
	}
	
	/**
	 * Generate a number of new one time keys.<br> If total number of keys stored
	 * by this account exceeds {@link #maxOneTimeKeys()}, the old keys are discarded.
	 * An exception is thrown if the operation fails.<br>
	 *
	 * @param aNumberOfKeys number of keys to generate
	 */
	private native void generateOneTimeKeysJni(int aNumberOfKeys);
	
	/**
	 * Return the "one time keys" in a dictionary.<br>
	 * The number of "one time keys", is specified by {@link #generateOneTimeKeys(int)}<br>
	 * Ex:<tt>
	 * { "curve25519":
	 * {
	 * "AAAABQ":"qefVZd8qvjOpsFzoKSAdfUnJVkIreyxWFlipCHjSQQg",
	 * "AAAABA":"/X8szMU+p+lsTnr56wKjaLgjTMQQkCk8EIWEAilZtQ8",
	 * "AAAAAw":"qxNxxFHzevFntaaPdT0fhhO7tc7pco4+xB/5VRG81hA",
	 * }
	 * }</tt><br>
	 * Public API for {@link #oneTimeKeysJni()}.<br>
	 * Note: these keys are to be published on the server.
	 *
	 * @return one time keys in string dictionary.
	 * @throws OlmException the failure reason
	 */
	public Map<String, Map<String, String>> oneTimeKeys()
			throws OlmException
	{
		JsonObject oneTimeKeysJsonObj = null;
		byte[] oneTimeKeysBuffer;
		
		try
		{
			oneTimeKeysBuffer = oneTimeKeysJni();
		}
		catch (Exception e)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_ONE_TIME_KEYS, e.getMessage());
		}
		
		if (null != oneTimeKeysBuffer)
		{
			try
			{
				oneTimeKeysJsonObj = (JsonObject) new Parser().parse(String.valueOf(oneTimeKeysBuffer));
			}
			catch (Exception e)
			{
				LOGGER.severe("## oneTimeKeys(): Exception - Msg=" + e.getMessage());
			}
		}
		else
		{
			LOGGER.severe("## oneTimeKeys(): Failure - identityKeysJni()=null");
		}
		
		return OlmUtility.toStringMapMap(oneTimeKeysJsonObj);
	}
	
	/**
	 * Get the public parts of the unpublished "one time keys" for the account.<br>
	 * The returned data is a JSON-formatted object with the single property
	 * <tt>curve25519</tt>, which is itself an object mapping key id to
	 * base64-encoded Curve25519 key.<br>
	 *
	 * @return byte array containing the one time keys or throw an exception if it fails
	 */
	private native byte[] oneTimeKeysJni();
	
	/**
	 * Remove the "one time keys" that the session used from the account.
	 *
	 * @param aSession session instance
	 * @throws OlmException the failure reason
	 */
	public void removeOneTimeKeys(OlmSession aSession)
			throws OlmException
	{
		if (null != aSession)
		{
			try
			{
				removeOneTimeKeysJni(aSession.getOlmSessionId());
			}
			catch (Exception e)
			{
				throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_REMOVE_ONE_TIME_KEYS, e.getMessage());
			}
		}
	}
	
	/**
	 * Remove the "one time keys" that the session used from the account.
	 * An exception is thrown if the operation fails.
	 *
	 * @param aNativeOlmSessionId native session instance identifier
	 */
	private native void removeOneTimeKeysJni(long aNativeOlmSessionId);
	
	/**
	 * Marks the current set of "one time keys" as being published.
	 *
	 * @throws OlmException the failure reason
	 */
	public void markOneTimeKeysAsPublished()
			throws OlmException
	{
		try
		{
			markOneTimeKeysAsPublishedJni();
		}
		catch (Exception e)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_MARK_ONE_KEYS_AS_PUBLISHED, e.getMessage());
		}
	}
	
	/**
	 * Marks the current set of "one time keys" as being published.
	 * An exception is thrown if the operation fails.
	 */
	private native void markOneTimeKeysAsPublishedJni();
	
	/**
	 * Sign a message with the ed25519 fingerprint key for this account.<br>
	 * The signed message is returned by the method.
	 *
	 * @param aMessage message to sign
	 * @return the signed message
	 * @throws OlmException the failure reason
	 */
	public String signMessage(String aMessage)
			throws OlmException
	{
		String result = null;
		
		if (null != aMessage)
		{
			try
			{
				byte[] utf8String = aMessage.getBytes("UTF-8");
				
				if (null != utf8String)
				{
					byte[] signedMessage = signMessageJni(utf8String);
					
					if (null != signedMessage)
					{
						result = new String(signedMessage, "UTF-8");
					}
				}
			}
			catch (Exception e)
			{
				throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_SIGN_MESSAGE, e.getMessage());
			}
		}
		
		return result;
	}
	
	/**
	 * Sign a message with the ed25519 fingerprint key for this account.<br>
	 * The signed message is returned by the method.
	 *
	 * @param aMessage message to sign
	 * @return the signed message
	 */
	private native byte[] signMessageJni(byte[] aMessage);
	
	//==============================================================================================================
	// Serialization management
	//==============================================================================================================
	
	/**
	 * Kick off the serialization mechanism.
	 *
	 * @param aOutStream output stream for serializing
	 * @throws IOException exception
	 */
	private void writeObject(ObjectOutputStream aOutStream)
			throws IOException
	{
		serialize(aOutStream);
	}
	
	/**
	 * Kick off the deserialization mechanism.
	 *
	 * @param aInStream input stream
	 * @throws Exception exception
	 */
	private void readObject(ObjectInputStream aInStream)
			throws Exception
	{
		deserialize(aInStream);
	}
	
	/**
	 * Return an account as a bytes buffer.<br>
	 * The account is serialized and encrypted with aKey.
	 * In case of failure, an error human readable
	 * description is provide in aErrorMsg.
	 *
	 * @param aKey      encryption key
	 * @param aErrorMsg error message description
	 * @return the account as bytes buffer
	 */
	@Override
	protected byte[] serialize(byte[] aKey, StringBuffer aErrorMsg)
	{
		byte[] pickleRetValue = null;
		
		// sanity check
		if (null == aErrorMsg)
		{
			LOGGER.severe("## serialize(): invalid parameter - aErrorMsg=null");
		}
		else if (null == aKey)
		{
			aErrorMsg.append("Invalid input parameters in serializeDataWithKey()");
		}
		else
		{
			aErrorMsg.setLength(0);
			try
			{
				pickleRetValue = serializeJni(aKey);
			}
			catch (Exception e)
			{
				LOGGER.severe("## serialize() failed " + e.getMessage());
				aErrorMsg.append(e.getMessage());
			}
		}
		
		return pickleRetValue;
	}
	
	/**
	 * Serialize and encrypt account instance.<br>
	 *
	 * @param aKeyBuffer key used to encrypt the serialized account data
	 * @return the serialised account as bytes buffer.
	 **/
	private native byte[] serializeJni(byte[] aKeyBuffer);
	
	/**
	 * Loads an account from a pickled bytes buffer.<br>
	 * See {@link #serialize(byte[], StringBuffer)}
	 *
	 * @param aSerializedData bytes buffer
	 * @param aKey            key used to encrypted
	 * @throws Exception the exception
	 */
	@Override
	protected void deserialize(byte[] aSerializedData, byte[] aKey)
			throws Exception
	{
		String errorMsg = null;
		
		try
		{
			if ((null == aSerializedData) || (null == aKey))
			{
				LOGGER.severe("## deserialize(): invalid input parameters");
				errorMsg = "invalid input parameters";
			}
			else
			{
				mNativeId = deserializeJni(aSerializedData, aKey);
			}
		}
		catch (Exception e)
		{
			LOGGER.severe("## deserialize() failed " + e.getMessage());
			errorMsg = e.getMessage();
		}
		
		if (!errorMsg.isEmpty())
		{
			releaseAccount();
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_DESERIALIZATION, errorMsg);
		}
	}
	
	/**
	 * Allocate a new account and initialize it with the serialisation data.<br>
	 *
	 * @param aSerializedDataBuffer the account serialisation buffer
	 * @param aKeyBuffer            the key used to encrypt the serialized account data
	 * @return the deserialized account
	 **/
	private native long deserializeJni(byte[] aSerializedDataBuffer, byte[] aKeyBuffer);
}
