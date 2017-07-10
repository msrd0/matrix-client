/*
 matrix-client-cli
 Copyright (C) 2017 Dominic Meiser
 
 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.
 
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/gpl-3.0>.
*/

package msrd0.matrix.client.cli;

import static java.util.concurrent.TimeUnit.*;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import msrd0.matrix.client.*;

import com.google.common.base.Stopwatch;
import org.slf4j.*;

public class Main
{
	private static final Logger logger = LoggerFactory.getLogger(Main.class);
	
	private static final BufferedReader sysin = new BufferedReader(new InputStreamReader(System.in));
	
	private static Properties conf = new Properties();

	private static String query(String q) throws IOException
	{
		System.out.print(q + ": ");
		System.out.flush();
		return sysin.readLine().trim();
	}

	private static Client client;
	
	public static void main(String args[]) throws Exception
	{
		Thread.setDefaultUncaughtExceptionHandler((t, ex) -> logger.error(ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex));
		
		// load the configuration if it exists
		File confFile = new File(System.getProperty("user.home"), ".matrix-client-cli.ini");
		if (confFile.exists())
			conf.load(new FileReader(confFile));

		// query homeserver information
		String hsDomain = conf.getProperty("hs.domain"), hsBase = conf.getProperty("hs.base");
		if (hsDomain == null)
		{
			hsDomain = query("HomeServer Domain");
			conf.setProperty("hs.domain", hsDomain);
		}
		if (hsBase == null)
		{
			hsBase = query("HomeServer Base URI");
			conf.setProperty("hs.base", hsBase);
		}
		HomeServer hs = new HomeServer(hsDomain, new URI(hsBase));
		
		// query user id information
		String idLocalpart = conf.getProperty("id.localpart"), idDomain = conf.getProperty("id.domain");
		if (idLocalpart == null)
		{
			idLocalpart = query("User ID Localpart");
			conf.setProperty("id.localpart", idLocalpart);
		}
		if (idDomain == null)
		{
			idDomain = query("User ID Domain");
			conf.setProperty("id.domain", idDomain);
		}
		MatrixId id = new MatrixId(idLocalpart, idDomain);
		
		// store the configuration
		conf.store(new FileWriter(confFile), null);
		
		// now create the client
		client = new Client(new ClientContext(hs, id));
		if (conf.containsKey("token"))
			client.setToken(conf.getProperty("token"));
		else
		{
			// the client has to login
			Auth a = client.auth(LoginType.PASSWORD);
			if (a == null)
			{
				System.out.println("Authentication failed");
				return;
			}
			a.setProperty("password", query("Password"));
			Collection<Auth> submit = a.submit();
			if (submit.stream().filter((o) -> o.getLoginType() == LoginType.SUCCESS).collect(Collectors.toList()).isEmpty())
			{
				System.out.println("Authentication failed");
				return;
			}
			if (client.getToken() == null)
			{
				System.out.println("No supported authentication method found");
				return;
			}
			conf.setProperty("token", client.getToken());
		}
		
		// store the configuration
		conf.store(new FileWriter(confFile), null);
		
		// synchronize the client
		sync();
		
		while (true)
		{
			System.out.print("> ");
			System.out.flush();
			String line = sysin.readLine().trim();
			if (line.isEmpty())
				continue;
			if (line.equals("q") || line.equals("quit"))
				break;
			
			if (line.equals("sync"))
				sync();
			
			else if (line.equals("rooms"))
			{
				ArrayList<Room> rooms = client.getRooms();
				for (Room room : rooms)
					System.out.println("  -> " + room.getName().toString() + " @ " + room.getId().getDomain());
			}
		}
		
		// store the configuration
		conf.store(new FileWriter(confFile), null);
	}
	
	private static void sync()
	{
		System.out.println("Synchronizing ...");
		Stopwatch stopwatch = Stopwatch.createStarted();
		client.sync();
		stopwatch.stop();
		System.out.println("Synchronization successfull (" + (stopwatch.elapsed(MILLISECONDS) / 1000.0) + " sec)");
	}
}
