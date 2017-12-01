package cn.apisium.authme.gui;

import java.lang.reflect.InvocationTargetException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import cn.apisium.authme.gui.window.AnvilLoginWindow;
import cn.apisium.authme.gui.window.LoginWindow;
import fr.xephi.authme.api.v3.AuthMeApi;

public class Main extends JavaPlugin implements Listener {

	private ProtocolManager protocolManager;
	private LoginWindow window;

	public void onLoad() {
		protocolManager = ProtocolLibrary.getProtocolManager();
	}

	@Override
	public void onEnable() {
		if (!this.getDataFolder().exists()) {
			this.getDataFolder().mkdirs();
		}
		this.saveDefaultConfig();
		this.getServer().getPluginManager().registerEvents(this, this);
		window = new AnvilLoginWindow(protocolManager, this);
		getLogger().info("AuthMe with GUI, Powered by Apisium.");
	}

	@Override
	public void onDisable() {
		getLogger().info("AuthMeGUI disabled.");
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onMove(PlayerMoveEvent event) {
		if (AuthMeApi.getInstance().isAuthenticated(event.getPlayer())) {
			return;
		}
		try {
			window.openFor(event.getPlayer());
		} catch (InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}
}
