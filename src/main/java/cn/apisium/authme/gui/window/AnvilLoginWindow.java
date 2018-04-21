package cn.apisium.authme.gui.window;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.SkullType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.StructureModifier;
import com.comphenix.protocol.wrappers.WrappedChatComponent;

import cn.apisium.authme.gui.variable.Variables;
import fr.xephi.authme.api.v3.AuthMeApi;
import fr.xephi.authme.events.LoginEvent;

public class AnvilLoginWindow extends LoginWindow {
	PacketAdapter click = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.WINDOW_CLICK) {
		@Override
		public void onPacketReceiving(PacketEvent event) {
			try {
				if (event.getPacketType() != PacketType.Play.Client.WINDOW_CLICK) {
					return;
				}
				PacketContainer packet = event.getPacket();
				StructureModifier<Integer> integers = packet.getIntegers();
				int windowID = integers.read(0);
				if (windowID != event.getPlayer().getName().hashCode() % 100 + 1) {
					return;
				}
				int slot = integers.read(1);
				if (slot == 2 || slot == 0) {
					String password = playerInput.containsKey(event.getPlayer().getName())
							? playerInput.get(event.getPlayer().getName())
							: "";
					boolean registered = AuthMeApi.getInstance().isRegistered(event.getPlayer().getName());
					String start = AnvilLoginWindow.this.getInfoFor(event.getPlayer(), Variables.anvilInfo.get(0));
					password = password.startsWith(start) ? password.substring(start.length()) : password;
					if (!registered & !password.isEmpty()) {
						AuthMeApi.getInstance().registerPlayer(event.getPlayer().getName(), password);
					}
					Bukkit.getPluginCommand("login").execute(event.getPlayer(), "login", new String[] { password });
				}
				try {
					AnvilLoginWindow.this.openFor(event.getPlayer());
				} catch (InvocationTargetException e) {
					if (Variables.debug)
						e.printStackTrace();
					throw new RuntimeException(e);
				}
			} catch (Throwable e) {
				if (Variables.debug)
					e.printStackTrace();
				// To avoid some strange issues
			}
		}
	};
	PacketAdapter itemName = new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.CUSTOM_PAYLOAD) {
		@Override
		public void onPacketReceiving(PacketEvent event) {
			try {
				if (event.getPacketType() != PacketType.Play.Client.CUSTOM_PAYLOAD) {
					return;
				}
				if (AuthMeApi.getInstance().isAuthenticated(event.getPlayer())) {
					return;
				}
				PacketContainer packet = event.getPacket();
				StructureModifier<String> strings = packet.getStrings();
				if (!strings.read(0).equals("MC|ItemName")) {
					return;
				}
				Object byteBuf = packet.getModifier().readSafely(1);
				try {
					Class<?> byteBufClass = byteBuf.getClass();
					boolean hasArray = (Boolean) byteBufClass.getDeclaredMethod("hasArray", new Class<?>[0])
							.invoke(byteBuf, new Object[0]);
					byte[] bytes;
					if (hasArray) {
						bytes = (byte[]) byteBufClass.getDeclaredMethod("array", new Class<?>[0]).invoke(byteBuf,
								new Object[0]);
					} else {
						int length = (Integer) byteBufClass.getDeclaredMethod("readableBytes", new Class<?>[0])
								.invoke(byteBuf, new Object[0]);
						bytes = new byte[length];
						int readerIndex = (Integer) byteBufClass.getDeclaredMethod("readerIndex", new Class<?>[0])
								.invoke(byteBuf, new Object[0]);
						byteBufClass.getDeclaredMethod("getBytes", new Class<?>[] { int.class, byte[].class })
								.invoke(byteBuf, new Object[] { readerIndex, bytes });
					}
					String contents;
					contents = new String(bytes, StandardCharsets.UTF_8).substring(1);
					playerInput.put(event.getPlayer().getName(), contents);

				} catch (Throwable e) {
					if (Variables.debug)
						e.printStackTrace();
				}
			} catch (Throwable e) {
				if (Variables.debug)
					e.printStackTrace();
				// To avoid some strange issues
			}
		}
	};

	public AnvilLoginWindow(ProtocolManager protocolManager, Plugin plugin) {
		super(protocolManager, plugin);
		protocolManager.addPacketListener(click);
		protocolManager.addPacketListener(itemName);
	}

	private HashMap<String, String> playerInput = new HashMap<String, String>();
	private HashMap<String, BukkitTask> taskMap = new HashMap<String, BukkitTask>();

	public void closeFor(final Player player) {
		final PacketContainer closeWindow = new PacketContainer(PacketType.Play.Server.CLOSE_WINDOW);
		closeWindow.getIntegers().write(0, player.getName().hashCode() % 100 + 1);
		try {
			protocolManager.sendServerPacket(player, closeWindow);
		} catch (InvocationTargetException e) {
			if (Variables.debug)
				e.printStackTrace();
		}
		return;

	}

	@SuppressWarnings("deprecation")
	@Override
	public void openFor(final Player player) throws InvocationTargetException {
		if (AuthMeApi.getInstance().isAuthenticated(player)) {
			return;
		}
		final int key = player.getName().hashCode() % 100 + 1;
		if (taskMap.containsKey(player.getName())) {
			taskMap.get(player.getName()).cancel();
		}
		final PacketContainer anvil = new PacketContainer(PacketType.Play.Server.OPEN_WINDOW);
		final PacketContainer slotAir = new PacketContainer(PacketType.Play.Server.SET_SLOT);
		// final PacketContainer slotHead = new
		// PacketContainer(PacketType.Play.Server.SET_SLOT);
		final PacketContainer slotHead = new PacketContainer(PacketType.Play.Server.WINDOW_ITEMS);
		slotAir.getIntegers().write(0, -1);
		slotAir.getIntegers().write(1, -1);
		slotAir.getItemModifier().write(0, new ItemStack(Material.AIR));
		anvil.getIntegers().write(0, key);
		anvil.getStrings().write(0, "minecraft:anvil");
		anvil.getChatComponents().write(0, WrappedChatComponent.fromText(getInfoFor(player, Variables.statueVariable)));
		anvil.getIntegers().write(1, 0);
		// slotHead.getIntegers().write(0, key);
		// slotHead.getIntegers().write(1, 0);
		ItemStack item = new ItemStack(Material.SKULL_ITEM, 1, (short) SkullType.PLAYER.ordinal());
		SkullMeta skullMeta = (SkullMeta) item.getItemMeta();
		boolean methodExist = false;
		try {
			for (Method method : skullMeta.getClass().getDeclaredMethods()) {
				if (method.getName().equals("setOwningPlayer")) {
					methodExist = true;
				}
			}
		} catch (Throwable e) {
			if (Variables.debug)
				e.printStackTrace();
		}
		if (methodExist) {
			skullMeta.setOwningPlayer(player);
		} else {
			skullMeta.setOwner(player.getName());
		}
		List<String> lines = this.getInfoFor(player);
		skullMeta.setDisplayName(lines.get(0));
		skullMeta.setLore(lines.subList(1, lines.size()));
		item.setItemMeta(skullMeta);
		// slotHead.getItemModifier().write(0, item);
		slotHead.getIntegers().write(0, key);
		List<ItemStack> list = new ArrayList<ItemStack>();
		list.add(item);
		try {
			slotHead.getItemListModifier().write(0, list);
		} catch (Throwable e) {
			if (Variables.debug)
				e.printStackTrace();
			slotHead.getItemArrayModifier().write(0, new ItemStack[] { item });
		}
		protocolManager.sendServerPacket(player, slotAir);
		protocolManager.sendServerPacket(player, anvil);
		Runnable sendSlot = new Runnable() {
			int count = 0;

			@Override
			public void run() {
				try {
					protocolManager.sendServerPacket(player, slotHead);
					String input = playerInput.get(player.getName());
					if (count < Variables.anvilLoginDelay)
						count++;
					else {
						playerInput.remove(player.getName());
						if (taskMap.containsKey(player.getName())) {
							taskMap.get(player.getName()).cancel();
						}
						taskMap.remove(player.getName());
						protocolManager = ProtocolLibrary.getProtocolManager();
						if (player.isOnline()) {
							closeFor(player);
							openFor(player);
						}
						return;
					}
					if (!AuthMeApi.getInstance().isAuthenticated(player)
							&& (input == null || (input.isEmpty() && !input.equals(Variables.anvilInfo.get(0)))))
						Bukkit.getScheduler().runTaskLater(plugin, this, Variables.anvilLoginDelay);
				} catch (InvocationTargetException e) {
					if (Variables.debug)
						e.printStackTrace();
				}

			}
		};
		taskMap.put(player.getName(), Bukkit.getScheduler().runTaskLater(plugin, sendSlot, Variables.anvilLoginDelay));
	}

	@EventHandler
	public void onAuthed(LoginEvent event) {
		closeFor(event.getPlayer());
	}

	@Override
	public List<String> getInfoFor(Player player) {
		List<String> info = new ArrayList<String>();
		for (String line : Variables.anvilInfo) {
			info.add(getInfoFor(player, line));
		}
		return info;
	}

}
