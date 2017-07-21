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

import org.slf4j.*;

/**
 * Session class used to create Olm sessions in conjunction with {@link OlmAccount} class.<br>
 * Olm session is used to encrypt data between devices, especially to create Olm group sessions (see {@link OlmOutboundGroupSession} and {@link OlmInboundGroupSession}).<br>
 * To establish an Olm session with Bob, Alice calls {@link #initOutboundSession(OlmAccount, String, String)} with Bob's identity and onetime keys. Then Alice generates an encrypted PRE_KEY message ({@link #encryptMessage(String)})
 * used by Bob to open the Olm session in his side with {@link #initOutboundSession(OlmAccount, String, String)}.
 * From this step on, messages can be exchanged by using {@link #encryptMessage(String)} and {@link #decryptMessage(OlmMessage)}.
 * <br><br>Detailed implementation guide is available at <a href="http://matrix.org/docs/guides/e2e_implementation.html">Implementing End-to-End Encryption in Matrix clients</a>.
 */
public class OlmSession extends CommonSerializeUtils implements Serializable
{
	private static final long serialVersionUID = -8975488639186976419L;
	private static final Logger LOGGER = LoggerFactory.getLogger(OlmSession.class);
	
	/**
	 * Session Id returned by JNI.
	 * This value uniquely identifies the native session instance.
	 **/
	private transient long mNativeId;
	
	public OlmSession()
			throws OlmException
	{
		try
		{
			mNativeId = createNewSessionJni();
		}
		catch (Exception e)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_INIT_SESSION_CREATION, e.getMessage());
		}
	}
	
	/**
	 * Create an OLM session in native side.<br>
	 * Do not forget to call {@link #releaseSession()} when JAVA side is done.
	 *
	 * @return native account instance identifier or throw an exception.
	 */
	private native long createNewSessionJni();
	
	/**
	 * Getter on the session ID.
	 *
	 * @return native session ID
	 */
	long getOlmSessionId()
	{
		return mNativeId;
	}
	
	/**
	 * Destroy the corresponding OLM session native object.<br>
	 * This method must ALWAYS be called when this JAVA instance
	 * is destroyed (ie. garbage collected) to prevent memory leak in native side.
	 * See {@link #createNewSessionJni()}.
	 */
	private native void releaseSessionJni();
	
	/**
	 * Release native session and invalid its JAVA reference counter part.<br>
	 * Public API for {@link #releaseSessionJni()}.
	 */
	public void releaseSession()
	{
		if (0 != mNativeId)
		{
			releaseSessionJni();
		}
		mNativeId = 0;
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
	 * Creates a new out-bound session for sending messages to a recipient
	 * identified by an identity key and a one time key.<br>
	 *
	 * @param aAccount          the account to associate with this session
	 * @param aTheirIdentityKey the identity key of the recipient
	 * @param aTheirOneTimeKey  the one time key of the recipient
	 * @throws OlmException the failure reason
	 */
	public void initOutboundSession(OlmAccount aAccount, String aTheirIdentityKey, String aTheirOneTimeKey)
			throws OlmException
	{
		if ((null == aAccount) || aTheirIdentityKey.isEmpty() || aTheirOneTimeKey.isEmpty())
		{
			LOGGER.error("## initOutboundSession(): invalid input parameters");
			throw new OlmException(OlmException.EXCEPTION_CODE_SESSION_INIT_OUTBOUND_SESSION, "invalid input parameters");
		}
		else
		{
			try
			{
				initOutboundSessionJni(aAccount.getOlmAccountId(), aTheirIdentityKey.getBytes("UTF-8"), aTheirOneTimeKey.getBytes("UTF-8"));
			}
			catch (Exception e)
			{
				LOGGER.error("## initOutboundSession(): " + e.getMessage());
				throw new OlmException(OlmException.EXCEPTION_CODE_SESSION_INIT_OUTBOUND_SESSION, e.getMessage());
			}
		}
	}
	
	/**
	 * Create a new in-bound session for sending/receiving messages from an
	 * incoming PRE_KEY message.<br> The recipient is defined as the entity
	 * with whom the session is established.
	 * An exception is thrown if the operation fails.
	 *
	 * @param aOlmAccountId     account instance
	 * @param aTheirIdentityKey the identity key of the recipient
	 * @param aTheirOneTimeKey  the one time key of the recipient
	 **/
	private native void initOutboundSessionJni(long aOlmAccountId, byte[] aTheirIdentityKey, byte[] aTheirOneTimeKey);
	
	/**
	 * Create a new in-bound session for sending/receiving messages from an
	 * incoming PRE_KEY message ({@link OlmMessage#MESSAGE_TYPE_PRE_KEY}).<br>
	 * This API may be used to process a "m.room.encrypted" event when type = 1 (PRE_KEY).
	 *
	 * @param aAccount   the account to associate with this session
	 * @param aPreKeyMsg PRE KEY message
	 * @throws OlmException the failure reason
	 */
	public void initInboundSession(OlmAccount aAccount, String aPreKeyMsg)
			throws OlmException
	{
		if ((null == aAccount) || aPreKeyMsg.isEmpty())
		{
			LOGGER.error("## initInboundSession(): invalid input parameters");
			throw new OlmException(OlmException.EXCEPTION_CODE_SESSION_INIT_INBOUND_SESSION, "invalid input parameters");
		}
		else
		{
			try
			{
				initInboundSessionJni(aAccount.getOlmAccountId(), aPreKeyMsg.getBytes("UTF-8"));
			}
			catch (Exception e)
			{
				LOGGER.error("## initInboundSession(): " + e.getMessage());
				throw new OlmException(OlmException.EXCEPTION_CODE_SESSION_INIT_INBOUND_SESSION, e.getMessage());
			}
		}
	}
	
	/**
	 * Create a new in-bound session for sending/receiving messages from an
	 * incoming PRE_KEY message.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aOlmAccountId  account instance
	 * @param aOneTimeKeyMsg PRE_KEY message
	 */
	private native void initInboundSessionJni(long aOlmAccountId, byte[] aOneTimeKeyMsg);
	
	/**
	 * Create a new in-bound session for sending/receiving messages from an
	 * incoming PRE_KEY({@link OlmMessage#MESSAGE_TYPE_PRE_KEY}) message based on the sender identity key.<br>
	 * Public API for {@link #initInboundSessionFromIdKeyJni(long, byte[], byte[])}.
	 * This API may be used to process a "m.room.encrypted" event when type = 1 (PRE_KEY).
	 * This method must only be called the first time a pre-key message is received from an inbound session.
	 *
	 * @param aAccount          the account to associate with this session
	 * @param aTheirIdentityKey the sender identity key
	 * @param aPreKeyMsg        PRE KEY message
	 * @throws OlmException the failure reason
	 */
	public void initInboundSessionFrom(OlmAccount aAccount, String aTheirIdentityKey, String aPreKeyMsg)
			throws OlmException
	{
		if ((null == aAccount) || aPreKeyMsg.isEmpty())
		{
			LOGGER.error("## initInboundSessionFrom(): invalid input parameters");
			throw new OlmException(OlmException.EXCEPTION_CODE_SESSION_INIT_INBOUND_SESSION_FROM, "invalid input parameters");
		}
		else
		{
			try
			{
				initInboundSessionFromIdKeyJni(aAccount.getOlmAccountId(), aTheirIdentityKey.getBytes("UTF-8"), aPreKeyMsg.getBytes("UTF-8"));
			}
			catch (Exception e)
			{
				LOGGER.error("## initInboundSessionFrom(): " + e.getMessage());
				throw new OlmException(OlmException.EXCEPTION_CODE_SESSION_INIT_INBOUND_SESSION_FROM, e.getMessage());
			}
		}
	}
	
	/**
	 * Create a new in-bound session for sending/receiving messages from an
	 * incoming PRE_KEY message based on the recipient identity key.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aOlmAccountId     account instance
	 * @param aTheirIdentityKey the identity key of the recipient
	 * @param aOneTimeKeyMsg    encrypted message
	 */
	private native void initInboundSessionFromIdKeyJni(long aOlmAccountId, byte[] aTheirIdentityKey, byte[] aOneTimeKeyMsg);
	
	/**
	 * Get the session identifier.<br> Will be the same for both ends of the
	 * conversation. The session identifier is returned as a String object.
	 * Session Id sample: "session_id":"M4fOVwD6AABrkTKl"
	 * Public API for {@link #getSessionIdentifierJni()}.
	 *
	 * @return the session ID
	 * @throws OlmException the failure reason
	 */
	public String sessionIdentifier()
			throws OlmException
	{
		try
		{
			byte[] buffer = getSessionIdentifierJni();
			
			if (null != buffer)
			{
				return new String(buffer, "UTF-8");
			}
		}
		catch (Exception e)
		{
			LOGGER.error("## sessionIdentifier(): " + e.getMessage());
			throw new OlmException(OlmException.EXCEPTION_CODE_SESSION_SESSION_IDENTIFIER, e.getMessage());
		}
		
		return null;
	}
	
	/**
	 * Get the session identifier for this session.
	 * An exception is thrown if the operation fails.
	 *
	 * @return the session identifier
	 */
	private native byte[] getSessionIdentifierJni();
	
	/**
	 * Checks if the PRE_KEY({@link OlmMessage#MESSAGE_TYPE_PRE_KEY}) message is for this in-bound session.<br>
	 * This API may be used to process a "m.room.encrypted" event when type = 1 (PRE_KEY).
	 * Public API for {@link #matchesInboundSessionJni(byte[])}.
	 *
	 * @param aOneTimeKeyMsg PRE KEY message
	 * @return true if the one time key matches.
	 */
	public boolean matchesInboundSession(String aOneTimeKeyMsg)
	{
		boolean retCode = false;
		
		try
		{
			retCode = matchesInboundSessionJni(aOneTimeKeyMsg.getBytes("UTF-8"));
		}
		catch (Exception e)
		{
			LOGGER.error("## matchesInboundSession(): failed " + e.getMessage());
		}
		
		return retCode;
	}
	
	/**
	 * Checks if the PRE_KEY message is for this in-bound session.<br>
	 * This API may be used to process a "m.room.encrypted" event when type = 1 (PRE_KEY).
	 * An exception is thrown if the operation fails.
	 *
	 * @param aOneTimeKeyMsg PRE KEY message
	 * @return true if the PRE_KEY message matches
	 */
	private native boolean matchesInboundSessionJni(byte[] aOneTimeKeyMsg);
	
	/**
	 * Checks if the PRE_KEY({@link OlmMessage#MESSAGE_TYPE_PRE_KEY}) message is for this in-bound session based on the sender identity key.<br>
	 * This API may be used to process a "m.room.encrypted" event when type = 1 (PRE_KEY).
	 * Public API for {@link #matchesInboundSessionJni(byte[])}.
	 *
	 * @param aTheirIdentityKey the sender identity key
	 * @param aOneTimeKeyMsg    PRE KEY message
	 * @return this if operation succeed, null otherwise
	 */
	public boolean matchesInboundSessionFrom(String aTheirIdentityKey, String aOneTimeKeyMsg)
	{
		boolean retCode = false;
		
		try
		{
			retCode = matchesInboundSessionFromIdKeyJni(aTheirIdentityKey.getBytes("UTF-8"), aOneTimeKeyMsg.getBytes("UTF-8"));
		}
		catch (Exception e)
		{
			LOGGER.error("## matchesInboundSessionFrom(): failed " + e.getMessage());
		}
		
		return retCode;
	}
	
	/**
	 * Checks if the PRE_KEY message is for this in-bound session based on the sender identity key.<br>
	 * This API may be used to process a "m.room.encrypted" event when type = 1 (PRE_KEY).
	 * An exception is thrown if the operation fails.
	 *
	 * @param aTheirIdentityKey the identity key of the sender
	 * @param aOneTimeKeyMsg    PRE KEY message
	 * @return true if the PRE_KEY message matches.
	 */
	private native boolean matchesInboundSessionFromIdKeyJni(byte[] aTheirIdentityKey, byte[] aOneTimeKeyMsg);
	
	/**
	 * Encrypt a message using the session.<br>
	 * The encrypted message is returned in a OlmMessage object.
	 * Public API for {@link #encryptMessageJni(byte[], OlmMessage)}.
	 *
	 * @param aClearMsg message to encrypted
	 * @return the encrypted message
	 * @throws OlmException the failure reason
	 */
	public OlmMessage encryptMessage(String aClearMsg)
			throws OlmException
	{
		if (null == aClearMsg)
		{
			return null;
		}
		
		OlmMessage encryptedMsgRetValue = new OlmMessage();
		
		try
		{
			byte[] encryptedMessageBuffer = encryptMessageJni(aClearMsg.getBytes("UTF-8"), encryptedMsgRetValue);
			
			if (null != encryptedMessageBuffer)
			{
				encryptedMsgRetValue.mCipherText = new String(encryptedMessageBuffer, "UTF-8");
			}
		}
		catch (Exception e)
		{
			LOGGER.error("## encryptMessage(): failed " + e.getMessage());
			throw new OlmException(OlmException.EXCEPTION_CODE_SESSION_ENCRYPT_MESSAGE, e.getMessage());
		}
		
		return encryptedMsgRetValue;
	}
	
	/**
	 * Encrypt a message using the session.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aClearMsg     clear text message
	 * @param aEncryptedMsg ciphered message
	 * @return the encrypted message
	 */
	private native byte[] encryptMessageJni(byte[] aClearMsg, OlmMessage aEncryptedMsg);
	
	/**
	 * Decrypt a message using the session.<br>
	 * The encrypted message is given as a OlmMessage object.
	 *
	 * @param aEncryptedMsg message to decrypt
	 * @return the decrypted message
	 * @throws OlmException the failure reason
	 */
	public String decryptMessage(OlmMessage aEncryptedMsg)
			throws OlmException
	{
		if (null == aEncryptedMsg)
		{
			return null;
		}
		
		try
		{
			return new String(decryptMessageJni(aEncryptedMsg), "UTF-8");
		}
		catch (Exception e)
		{
			LOGGER.error("## decryptMessage(): failed " + e.getMessage());
			throw new OlmException(OlmException.EXCEPTION_CODE_SESSION_DECRYPT_MESSAGE, e.getMessage());
		}
	}
	
	/**
	 * Decrypt a message using the session.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aEncryptedMsg message to decrypt
	 * @return the decrypted message
	 */
	private native byte[] decryptMessageJni(OlmMessage aEncryptedMsg);
	
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
	 * @throws IOException            exception
	 * @throws ClassNotFoundException exception
	 */
	private void readObject(ObjectInputStream aInStream)
			throws Exception
	{
		deserialize(aInStream);
	}
	
	/**
	 * Return a session as a bytes buffer.<br>
	 * The account is serialized and encrypted with aKey.
	 * In case of failure, an error human readable
	 * description is provide in aErrorMsg.
	 *
	 * @param aKey      encryption key
	 * @param aErrorMsg error message description
	 * @return session as a bytes buffer
	 */
	@Override
	protected byte[] serialize(byte[] aKey, StringBuffer aErrorMsg)
	{
		byte[] pickleRetValue = null;
		
		// sanity check
		if (null == aErrorMsg)
		{
			LOGGER.error("## serializeDataWithKey(): invalid parameter - aErrorMsg=null");
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
				LOGGER.error("## serializeDataWithKey(): failed " + e.getMessage());
				aErrorMsg.append(e.getMessage());
			}
		}
		
		return pickleRetValue;
	}
	
	/**
	 * Serialize and encrypt session instance.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aKeyBuffer key used to encrypt the serialized account data
	 * @return the serialised account as bytes buffer.
	 **/
	private native byte[] serializeJni(byte[] aKeyBuffer);
	
	/**
	 * Loads an account from a pickled base64 string.<br>
	 * See {@link #serialize(byte[], StringBuffer)}
	 *
	 * @param aSerializedData pickled account in a base64 string format
	 * @param aKey            key used to encrypted
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
				LOGGER.error("## deserialize(): invalid input parameters");
				errorMsg = "invalid input parameters";
			}
			else
			{
				mNativeId = deserializeJni(aSerializedData, aKey);
			}
		}
		catch (Exception e)
		{
			LOGGER.error("## deserialize() failed " + e.getMessage());
			errorMsg = e.getMessage();
		}
		
		if (!errorMsg.isEmpty())
		{
			releaseSession();
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_DESERIALIZATION, errorMsg);
		}
	}
	
	/**
	 * Allocate a new session and initialize it with the serialisation data.<br>
	 * An exception is thrown if the operation fails.
	 *
	 * @param aSerializedData the session serialisation buffer
	 * @param aKey            the key used to encrypt the serialized account data
	 * @return the deserialized session
	 **/
	private native long deserializeJni(byte[] aSerializedData, byte[] aKey);
}

