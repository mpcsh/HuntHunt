package mpcsh.hunthunt;


import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;


public class UpdateCompassTask extends BukkitRunnable {
	private HuntHunt plugin;

	public UpdateCompassTask(HuntHunt plugin) {
		this.plugin = plugin;
	}

	@Override
	public void run() {
		for (Map.Entry<UUID, UUID> entry : this.plugin.getPointing().entrySet()) {
			Player player = Bukkit.getPlayer(entry.getKey());
			Player enemy = Bukkit.getPlayer(entry.getValue());

			if (enemy != null) {
				player.setCompassTarget(enemy.getLocation());
			}
		}
	}
}
