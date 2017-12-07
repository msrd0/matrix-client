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
import static org.matrix.olm.OlmException.*;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javax.annotation.*;

import msrd0.matrix.client.RoomId;
import msrd0.matrix.client.e2e.olm.KeyStore;

import kotlin.Pair;
import org.matrix.olm.*;

public class PropertiesKeyStore implements KeyStore
{
	@Nonnull
	private static String serializeToString(@Nonnull Object obj)
			throws IOException
	{
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bytes);
		out.writeObject(obj);
		out.close();
		return toBase64(bytes.toByteArray());
	}
	
	@Nonnull
	private static Object deserializeFromString(@Nonnull String str)
			throws IOException, ClassNotFoundException
	{
		ByteArrayInputStream bytes = new ByteArrayInputStream(fromBase64(str));
		ObjectInputStream in = new ObjectInputStream(bytes);
		Object obj = in.readObject();
		in.close();
		return obj;
	}
	
	private static DateTimeFormatter df = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
	
	@Nonnull
	private final Properties properties;
	
	public PropertiesKeyStore(@Nonnull Properties properties)
	{
		this.properties = properties;
	}
	
	@Nonnull
	@Override
	public OlmAccount getAccount() throws OlmException
	{
		final String str = properties.getProperty("olm.account", "");
		if (str.isEmpty())
			throw new IllegalStateException();
		try
		{
			return (OlmAccount) deserializeFromString(str);
		}
		catch (Exception ex)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_DESERIALIZATION, ex.getMessage());
		}
	}
	
	@Override
	public void setAccount(@Nonnull OlmAccount olmAccount) throws OlmException
	{
		String str;
		try
		{
			str = serializeToString(olmAccount);
		}
		catch (Exception ex)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_SERIALIZATION, ex.getMessage());
		}
		properties.setProperty("olm.account", str);
	}
	
	@Override
	public boolean hasAccount()
	{
		return !properties.getProperty("olm.account", "").isEmpty();
	}
	
	
	@Override
	public void storeSession(@Nonnull OlmSession session)
			throws OlmException
	{
		String str;
		try
		{
			str = serializeToString(session);
		}
		catch (Exception ex)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_SERIALIZATION, ex.getMessage());
		}
		properties.setProperty("olm.session." + session.sessionIdentifier(), str);
	}
	
	@Nullable
	@Override
	public OlmSession findSession(@Nonnull String sessionId)
			throws OlmException
	{
		final String str = properties.getProperty("olm.session." + sessionId);
		if (str.isEmpty())
			return null;
		OlmSession session;
		try
		{
			session = (OlmSession) deserializeFromString(str);
		}
		catch (Exception ex)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_DESERIALIZATION, ex.getMessage());
		}
		return session;
	}
	
	@Nonnull
	@Override
	public Collection<OlmSession> allSessions()
			throws OlmException
	{
		final List<String> keys = properties.keySet().stream()
				.map((key) -> (String)key)
				.filter((key) -> key.startsWith("olm.session."))
				.map((key) -> key.substring(12))
				.collect(Collectors.toList());
		final List<OlmSession> sessions = new ArrayList<>(keys.size());
		for (String key : keys)
			sessions.add(findSession(key));
		return sessions;
	}
	
	
	@Override
	public void storeOutboundSession(@Nonnull RoomId room, @Nonnull OlmOutboundGroupSession session, @Nonnull LocalDateTime timestamp)
			throws OlmException
	{
		String str;
		try
		{
			str = serializeToString(session);
		}
		catch (Exception ex)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_SERIALIZATION, ex.getMessage());
		}
		properties.setProperty("olm.outbound." + room, str);
		properties.setProperty("olm.outbound." + room + ".timestamp", df.format(timestamp));
	}
	
	@Nullable
	@Override
	public Pair<OlmOutboundGroupSession, LocalDateTime> findOutboundSession(@Nonnull RoomId room)
			throws OlmException
	{
		final String str = properties.getProperty("olm.outbound." + room, "");
		if (str.isEmpty())
			return null;
		OlmOutboundGroupSession outbound;
		try
		{
			outbound = (OlmOutboundGroupSession) deserializeFromString(str);
		}
		catch (Exception ex)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_DESERIALIZATION, ex.getMessage());
		}
		final LocalDateTime timestamp = LocalDateTime.parse(properties.getProperty("olm.outbound." + room + ".timestamp"), df);
		return new Pair<>(outbound, timestamp);
	}
	
	
	@Override
	public void storeInboundSession(@Nonnull OlmInboundGroupSession session)
			throws OlmException
	{
		String str;
		try
		{
			str = serializeToString(session);
		}
		catch (Exception ex)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_SERIALIZATION, ex.getMessage());
		}
		properties.setProperty("olm.inbound." + session.sessionIdentifier(), str);
	}
	
	@Nullable
	@Override
	public OlmInboundGroupSession findInboundSession(@Nonnull String sessionId)
			throws OlmException
	{
		final String str = properties.getProperty("olm.inbound." + sessionId, "");
		if (str.isEmpty())
			return null;
		OlmInboundGroupSession inbound;
		try
		{
			inbound = (OlmInboundGroupSession) deserializeFromString(str);
		}
		catch (Exception ex)
		{
			throw new OlmException(EXCEPTION_CODE_ACCOUNT_DESERIALIZATION, ex.getMessage());
		}
		return inbound;
	}
}
