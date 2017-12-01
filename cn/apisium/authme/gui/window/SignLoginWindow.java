package cn.apisium.authme.gui.window;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.BlockPosition;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import com.comphenix.protocol.wrappers.nbt.NbtCompound;
import com.comphenix.protocol.wrappers.nbt.NbtFactory;

import fr.xephi.authme.api.v3.AuthMeApi;

public class SignLoginWindow extends LoginWindow {

	public SignLoginWindow(ProtocolManager protocolManager, Plugin plugin) {
		super(protocolManager, plugin);
		protocolManager.addPacketListener(
				new PacketAdapter(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.UPDATE_SIGN) {
					@Override
					public void onPacketReceiving(PacketEvent event) {
						if (event.getPacketType() == PacketType.Play.Client.UPDATE_SIGN) {
							PacketContainer packet = event.getPacket();
							String[] strings = packet.getStringArrays().read(0);
							if (!strings[0].equals(event.getPlayer().getDisplayName())) {
								return;
							}
							if (strings[2].isEmpty()) {
								return;
							}
							boolean registered = AuthMeApi.getInstance().isRegistered(event.getPlayer().getName());
							String password = strings[2];
							if (!registered & !password.isEmpty()) {
								AuthMeApi.getInstance().registerPlayer(event.getPlayer().getName(), password);
							}
							Bukkit.getPluginCommand("login").execute(event.getPlayer(), "login",
									new String[] { password });
						}
					}
				});
	}

	@Override
	public void openFor(Player player) throws InvocationTargetException {
		if (AuthMeApi.getInstance().isAuthenticated(player)) {
			return;
		}
		PacketContainer fakeBlockChange = new PacketContainer(PacketType.Play.Server.BLOCK_CHANGE);
		PacketContainer updateEntity = new PacketContainer(PacketType.Play.Server.TILE_ENTITY_DATA);
		PacketContainer sign = new PacketContainer(PacketType.Play.Server.OPEN_SIGN_EDITOR);
		Location block = player.getLocation().getBlock().getLocation();
		BlockPosition position = new BlockPosition(block.getBlockX(), block.getBlockY() + 1, block.getBlockZ());
		fakeBlockChange.getBlockPositionModifier().write(0, position);
		fakeBlockChange.getBlockData().write(0, WrappedBlockData.createData(Material.SIGN_POST));
		protocolManager.sendServerPacket(player, fakeBlockChange);
		updateEntity.getBlockPositionModifier().write(0, position);
		updateEntity.getIntegers().write(0, 9);
		NbtCompound signNbt = (NbtCompound) updateEntity.getNbtModifier().read(0);
		signNbt = signNbt == null ? NbtFactory.ofCompound("") : signNbt;
		signNbt.put("Text1", "{\"text\":\"" + player.getDisplayName() + "\"}");
		signNbt.put("Text2", "{\"text\":\"请在下一行输入密码：\"}");
		signNbt.put("Text3", "{\"text\":\"\"}");
		boolean registered = AuthMeApi.getInstance().isRegistered(player.getName());
		signNbt.put("Text4", "{\"text\":\"点击完成进行" + (registered ? "登录" : "注册") + "\"}");
		updateEntity.getNbtModifier().write(0, signNbt);
		protocolManager.sendServerPacket(player, updateEntity);
		sign.getBlockPositionModifier().write(0, position);
		protocolManager.sendServerPacket(player, sign);
	}

}
