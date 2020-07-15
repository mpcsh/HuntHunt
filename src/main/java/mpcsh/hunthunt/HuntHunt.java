package mpcsh.hunthunt;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;


public class HuntHunt extends JavaPlugin implements Listener, CommandExecutor {
	private List<UUID> redTeam;
	private List<UUID> blueTeam;
	private Map<UUID, UUID> pointing;


	public void onEnable() {
		getServer().getPluginManager().registerEvents(this, this);

		for (String command : getDescription().getCommands().keySet()) {
			getServer().getPluginCommand(command).setExecutor(this);
		}

		this.redTeam = new ArrayList<>();
		this.blueTeam = new ArrayList<>();
		this.pointing = new HashMap<>();

		UpdateCompassTask task = new UpdateCompassTask(this);
		task.runTaskTimer(this, 0, 10);
	}


	private void addCompassIfNotPresent(Player player) {
		Inventory inventory = player.getInventory();
		if (!inventory.contains(Material.COMPASS)) {
			inventory.addItem(new ItemStack(Material.COMPASS));
		}
	}


	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!command.getName().equalsIgnoreCase("team")) {
			sender.sendMessage(ChatColor.YELLOW + "Invalid command " + command.getName() + "!");
			return false;
		}

		if (args.length != 2) {
			sender.sendMessage(ChatColor.YELLOW + "Invalid number of arguments!");
			return false;
		}

		Player player = Bukkit.getPlayer(args[0]);
		if (player == null) {
			sender.sendMessage(ChatColor.YELLOW + "Player " + args[0] + " not found!");
			return true;
		}

		if (args[1].equalsIgnoreCase("red")) {
			this.blueTeam.remove(player.getUniqueId());
			this.redTeam.add(player.getUniqueId());
			addCompassIfNotPresent(player);
			Bukkit.broadcastMessage(ChatColor.RED + player.getName() + " is now on the red team");
		}

		else if (args[1].equalsIgnoreCase("blue")) {
			this.redTeam.remove(player.getUniqueId());
			this.blueTeam.add(player.getUniqueId());
			addCompassIfNotPresent(player);
			Bukkit.broadcastMessage(ChatColor.BLUE + player.getName() + " is now on the blue team");
		}

		else if (args[1].equalsIgnoreCase("none")) {
			this.redTeam.remove(player.getUniqueId());
			this.blueTeam.remove(player.getUniqueId());
			this.pointing.put(player.getUniqueId(), null);
			player.getInventory().remove(Material.COMPASS);
			Bukkit.broadcastMessage(ChatColor.GRAY + player.getName() + " is now neutral");
		}

		else {
			sender.sendMessage(ChatColor.YELLOW + "Invalid team " + args[1] + "!");
		}

		return true;
	}


	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (!(event.hasItem() && event.getItem().getType() == Material.COMPASS && (event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.RIGHT_CLICK_AIR))) {
			return;
		}

		Player player = event.getPlayer();

		List<UUID> enemyTeam;

		if (this.redTeam.contains(player.getUniqueId())) {
			enemyTeam = this.blueTeam;
		}

		else if (this.blueTeam.contains(player.getUniqueId())) {
			enemyTeam = this.redTeam;
		}

		else {
			player.sendMessage(ChatColor.YELLOW + "Not on a team!");
			return;
		}

		if (enemyTeam.isEmpty()) {
			player.sendMessage(ChatColor.YELLOW + "No players to track!");
			return;
		}

		UUID currentEnemyUUID = this.pointing.get(player.getUniqueId());
		Integer currentEnemyIndex = enemyTeam.indexOf(currentEnemyUUID);

		Integer nextEnemyIndex = (currentEnemyIndex + 1) % enemyTeam.size();
		UUID nextEnemyUUID = enemyTeam.get(nextEnemyIndex);
		Player nextEnemy = Bukkit.getPlayer(nextEnemyUUID);

		this.pointing.put(player.getUniqueId(), nextEnemyUUID);

		player.sendMessage(ChatColor.GREEN + "Compass is now pointing to " + nextEnemy.getName());
	}


	@EventHandler
	public void onPlayerDeathEvent(PlayerDeathEvent event) {
		UUID id = event.getEntity().getUniqueId();
		if (this.redTeam.contains(id) || this.blueTeam.contains(id)) {
			event.getDrops().removeIf(next -> (next.getType() == Material.COMPASS));
		}
	}


	@EventHandler
	public void onPlayerDropItemEvent(PlayerDropItemEvent event) {
		UUID id = event.getPlayer().getUniqueId();
		if ((this.redTeam.contains(id) || this.blueTeam.contains(id)) && event.getItemDrop().getItemStack().getType() == Material.COMPASS) {
			event.setCancelled(true);
		}
	}


	@EventHandler
	public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		UUID id = player.getUniqueId();
		if (this.redTeam.contains(id) || this.blueTeam.contains(id)) {
			player.getInventory().addItem(new ItemStack(Material.COMPASS));
		}
	}

	public Map<UUID, UUID> getPointing() {
		return this.pointing;
	}
}
