/*
 * matrix-client-cli
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

import static java.util.concurrent.TimeUnit.*;
import static msrd0.matrix.client.listener.EventTypes.*;

import java.io.*;
import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

import msrd0.matrix.client.*;
import msrd0.matrix.client.e2e.MatrixE2EException;
import msrd0.matrix.client.e2e.olm.OlmE2E;
import msrd0.matrix.client.event.*;
import msrd0.matrix.client.listener.*;

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

	private static MatrixClient client;
	
	private static void sendMessage(Room room, MessageContent msg)
			throws MatrixAnswerException, MatrixE2EException
	{
		if (room.isEncrypted())
			room.sendEncryptedMessage(msg);
		else
			room.sendMessage(msg);
	}
	
	public static void main(String args[]) throws Exception
	{
		Thread.setDefaultUncaughtExceptionHandler((t, ex) -> {
			logger.error(ex.getClass().getSimpleName() + ": " + ex.getMessage(), ex);
			System.exit(1);
		});
		
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
		client = new MatrixClient(hs, id);
		if (conf.containsKey("token") && conf.containsKey("deviceId"))
			client.setUserData(new MatrixUserData(conf.getProperty("token"), conf.getProperty("deviceId")));
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
			conf.setProperty("deviceId", client.getDeviceId());
		}
		
		// load the last txn id
		if (conf.containsKey("txnid"))
			client.setLastTxnId(Long.parseLong(conf.getProperty("txnid")));
		
		// store the configuration
		conf.store(new FileWriter(confFile), null);
		
		// register some listeners
		RoomJoinListener joinListener = (ev) -> {
			System.out.println("Joined a new room: " + ev.getRoom());
			return true;
		};
		client.on(ROOM_JOIN, joinListener);
		RoomInvitationListener invitationListener = (ev) -> {
			System.out.println("Invited to a new room: " + ev.getRoom());
			return true;
		};
		client.on(ROOM_INVITATION, invitationListener);
		RoomMessageReceivedListener messageListener = (ev) -> {
			System.out.println("New message in room " + ev.getRoom().getName());
			try
			{
				printMessage(ev.getMsg());
			}
			catch (IOException e)
			{
				e.printStackTrace(); // #javaIsSoUgly
			}
			return true;
		};
		client.on(ROOM_MESSAGE_RECEIVED, messageListener);
		
		// initialize e2e
		client.enableE2E(new OlmE2E(new PropertiesKeyStore(conf)));
		client.uploadIdentityKeys();
		client.startUpdateOneTimeKeysBlocking();
		
		// set a cool display name for our device
		client.updateDeviceDisplayName(client.getDeviceId(), "Mextrix CLI Matrix Client");
		
		// synchronize the client
		sync();
		// run blocking synchronization in a coroutine
		client.startSyncBlocking();
		
		Room curr = null;
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
				Collection<Room> rooms = client.getRooms();
				for (Room room : rooms)
					System.out.println("  -> " + room.getName() + " @ " + room.getId().getDomain());
			}
			
			else if (line.startsWith("accept "))
			{
				final String name = line.substring(7);
				RoomInvitation room = client.getRoomsInvited().stream().filter(i -> i.getRoom().toString().equals(name)).collect(Collectors.toList()).get(0);
				if (room == null)
					System.out.println("Invitation " + name + " not found");
				else
					room.accept();
			}
			
			else if (line.startsWith("room "))
			{
				final String name = line.substring(5);
				curr = client.getRooms().stream().filter(room -> room.getName().equals(name)).collect(Collectors.toList()).get(0);
				if (curr == null)
					System.out.println("Room " + name + " not found");
				else
				{
					for (MatrixId user : curr.getMembers())
						System.out.println("  -> Member " + user + " (" + client.presence(user).getPresence().name() + ")");
					printMessages(curr);
				}
			}
			
			else if (line.equals("messages"))
			{
				if (curr == null)
					System.out.println("No room selected");
				else
				{
					printMessages(curr);
				}
			}
			else if (line.equals("send"))
			{
				if (curr == null)
					System.out.println("No room selected");
				else
				{
					MessageContent content = new TextMessageContent(query("Message"));
					sendMessage(curr, content);
				}
			}
			else if (line.equals("sendimg"))
			{
				if (curr == null)
					System.out.println("No room selected");
				else
				{
					String path = query("Image Path");
					File file = new File(path);
					ImageMessageContent content = new ImageMessageContent(file.getName());
					content.uploadImage(ImageIO.read(file), client);
					sendMessage(curr, content);
				}
			}
		}
		
		// update the txn id
		conf.setProperty("txnid", Long.toString(client.getLastTxnId()));
		
		// store the configuration
		conf.store(new FileWriter(confFile), null);
		
		// stop the client's event queue
		MatrixClient.stopEventQueue();
		
		// exit the system
		System.exit(0);
	}
	
	private static void sync() throws MatrixAnswerException
	{
		System.out.println("Synchronizing ...");
		Stopwatch stopwatch = Stopwatch.createStarted();
		client.sync();
		stopwatch.stop();
		System.out.println("Synchronization successfull (" + (stopwatch.elapsed(MILLISECONDS) / 1000.0) + " sec)");
	}
	
	private static void printMessage(Message msg) throws IOException
	{
		String body = msg.getBody();
		if (msg.getMsgtype().equals(MessageTypes.IMAGE))
		{
			try
			{
				File file = File.createTempFile("matrix", ".png");
				file.deleteOnExit();
				ImageIO.write(((ImageMessageContent) msg.getContent()).downloadImage(client), "PNG", file);
				body = "(" + file.getAbsolutePath() + ") " + body;
			}
			catch (MatrixAnswerException mae)
			{
				body = "(broken image) " + body;
			}
		}
		System.out.println("  -> " + msg.getTimestamp().format(DateTimeFormatter.ofPattern("dd MMM uuuu HH:mm:ss")) +
				" [" + msg.getSender() + "] " + body);
	}
	
	private static void printMessages(Room room) throws MatrixAnswerException, IOException
	{
		Messages msgs = room.retrieveMessages();
		for (Message msg : msgs)
			printMessage(msg);
	}
}
