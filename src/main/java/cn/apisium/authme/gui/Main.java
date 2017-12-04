package cn.apisium.authme.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;

import cn.apisium.authme.gui.variable.NonConfig;
import cn.apisium.authme.gui.variable.Variables;
import cn.apisium.authme.gui.window.AnvilLoginWindow;
import cn.apisium.authme.gui.window.LoginWindow;
import cn.apisium.authme.gui.window.SignLoginWindow;
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
		loadConfig();
		if (Variables.windowType.equalsIgnoreCase("sign")) {
			window = new SignLoginWindow(protocolManager, this);
		} else if (Variables.windowType.equalsIgnoreCase("anvil")) {
			window = new AnvilLoginWindow(protocolManager, this);
		} else {
			window = new AnvilLoginWindow(protocolManager, this);
		}
		this.getServer().getPluginManager().registerEvents(this, this);
		this.getServer().getPluginManager().registerEvents(window, this);
		getLogger().info("AuthMe with GUI, Powered by Apisium.");
	}

	public Main loadConfig() {
		Variables.scriptPath = Variables.scriptPath.isEmpty()
				? new File(this.getDataFolder(), "script.js").getAbsolutePath()
				: Variables.scriptPath;
		Class<?> variables = Variables.class;
		for (Field variable : variables.getDeclaredFields()) {
			if (variable.isAnnotationPresent(NonConfig.class)) {
				continue;
			}
			String path = capitalFirst(variable.getName());
			try {
				Object defaultValue = variable.get(null);
				Object config = this.getConfig().get(path, null);
				if (config != null)
					variable.set(null, config);
				else if (defaultValue != null)
					this.getConfig().set(path, defaultValue);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			}
		}
		this.applyScript();
		this.saveConfig();
		return this;
	}

	public String capitalFirst(String string) {
		char[] cs = string.toCharArray();
		cs[0] -= 32;
		return String.valueOf(cs);
	}

	public void applyScript() {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(Variables.scriptPath));
			try {
				StringBuilder builder = new StringBuilder();
				String line = reader.readLine();

				while (line != null) {
					builder.append(line);
					builder.append(System.lineSeparator());
					line = reader.readLine();
				}
				Variables.script = builder.toString();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (FileNotFoundException e) {
		}
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
