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
import java.util.logging.Logger;

/**
 * Helper class dedicated to serialization mechanism (template method pattern).
 */
abstract class CommonSerializeUtils
{
	private static final Logger LOGGER = Logger.getLogger(CommonSerializeUtils.class.getName());
	
	/**
	 * Kick off the serialization mechanism.
	 *
	 * @param aOutStream output stream for serializing
	 * @throws IOException exception
	 */
	protected void serialize(ObjectOutputStream aOutStream)
			throws IOException
	{
		aOutStream.defaultWriteObject();
		
		// generate serialization key
		byte[] key = OlmUtility.getRandomKey();
		
		// compute pickle string
		StringBuffer errorMsg = new StringBuffer();
		byte[] pickledData = serialize(key, errorMsg);
		
		if (null == pickledData)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_SERIALIZATION, String.valueOf(errorMsg));
		}
		else
		{
			aOutStream.writeObject(new String(key, "UTF-8"));
			aOutStream.writeObject(new String(pickledData, "UTF-8"));
		}
	}
	
	/**
	 * Kick off the deserialization mechanism.
	 *
	 * @param aInStream input stream
	 * @throws Exception the exception
	 */
	protected void deserialize(ObjectInputStream aInStream)
			throws Exception
	{
		aInStream.defaultReadObject();
		
		String keyAsString = (String) aInStream.readObject();
		String pickledDataAsString = (String) aInStream.readObject();
		
		byte[] key;
		byte[] pickledData;
		
		try
		{
			key = keyAsString.getBytes("UTF-8");
			pickledData = pickledDataAsString.getBytes("UTF-8");
			
			deserialize(pickledData, key);
		}
		catch (Exception e)
		{
			throw new OlmException(OlmException.EXCEPTION_CODE_ACCOUNT_DESERIALIZATION, e.getMessage());
		}
		
		LOGGER.info("## deserializeObject(): success");
	}
	
	protected abstract byte[] serialize(byte[] aKey, StringBuffer aErrorMsg);
	
	protected abstract void deserialize(byte[] aSerializedData, byte[] aKey)
			throws Exception;
}
