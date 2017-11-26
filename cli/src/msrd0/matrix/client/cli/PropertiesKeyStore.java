/*
 * matrix-client
 * Copyright (C) 2017 Dominic Meiser
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/gpl-3.0>.
 */

package msrd0.matrix.client.cli;

import static msrd0.matrix.client.util.EncodingUtil.*;
import static org.matrix.olm.OlmException.EXCEPTION_CODE_ACCOUNT_SERIALIZATION;

import java.io.*;
import java.util.*;
import javax.annotation.Nonnull;

import msrd0.matrix.client.e2e.KeyStore;
import msrd0.matrix.client.util.EncodingUtil;

import org.matrix.olm.*;

public class PropertiesKeyStore implements KeyStore
{
	private final Properties properties;
	
	public PropertiesKeyStore(@Nonnull Properties properties)
	{
		this.properties = properties;
	}
	
	@Nonnull
	@Override
	public OlmAccount getAccount() throws OlmException
	{
		byte[] bytes = fromBase64(properties.getProperty("olm.account", ""));
		if (bytes.length == 0)
			throw new IllegalStateException();
		try
		{
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
			return (OlmAccount) in.readObject();
		}
		catch (Exception ex)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_SERIALIZATION, ex.getMessage());
		}
	}
	
	@Override
	public void setAccount(@Nonnull OlmAccount olmAccount) throws OlmException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		try (ObjectOutputStream out = new ObjectOutputStream(bytes))
		{
			out.writeObject(olmAccount);
		}
		catch (Exception ex)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_SERIALIZATION, ex.getMessage());
		}
		properties.setProperty("olm.account", toBase64(bytes.toByteArray()));
	}
	
	@Override
	public boolean getHasAccount()
	{
		return !properties.getProperty("olm.account", "").isEmpty();
	}
}
