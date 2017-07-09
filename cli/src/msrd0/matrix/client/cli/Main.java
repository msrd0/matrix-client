package msrd0.matrix.client.cli;

import msrd0.matrix.client.*;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class Main
{
	public static Properties conf = new Properties();

	public static String query(String q) throws IOException
	{
		System.out.print(q + ": ");
		System.out.flush();
		return new BufferedReader(new InputStreamReader(System.in)).readLine().trim();
	}

	public static void main(String args[]) throws Exception
	{
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
		Client client = new Client(new ClientContext(hs, id));
		if (conf.containsKey("token"))
			client.setToken(conf.getProperty("token"));
		else
		{
			// the client has to login
			Collection<Auth> auth = client.auth();
			for (Auth a : auth)
			{
				if (a.getLoginType() == LoginType.PASSWORD)
				{
					a.setProperty("password", query("Password"));
					Collection<Auth> submit = a.submit();
					if (submit.stream().filter((o) -> o.getLoginType() == LoginType.SUCCESS).collect(Collectors.toList()).isEmpty())
					{
						System.out.println("Authentication failed");
						return;
					}
					conf.setProperty("token", client.getToken());
					break;
				}
			}
			if (client.getToken() == null)
			{
				System.out.println("No supported authentication method found");
				return;
			}
		}
		
		// store the configuration
		conf.store(new FileWriter(confFile), null);
		
		// synchronize the client
		client.sync();
		System.out.println("Synchronization successfull");
		
		// store the configuration
		conf.store(new FileWriter(confFile), null);
	}
}
