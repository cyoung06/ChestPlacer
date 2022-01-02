package kr.syeyoung.randomChest;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.material.Chest;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.command.Command;
import org.bukkit.plugin.java.annotation.command.Commands;
import org.bukkit.plugin.java.annotation.plugin.Plugin;

;import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

@Plugin(name="ChestPlacer", version="1.0")
@Commands(@Command(name = "placeChest", aliases = "placechest", usage = "/testext test test"))
public class Main extends JavaPlugin {
    @Override
    public void onEnable() {

    }

    @Override
    public boolean onCommand(CommandSender sender, org.bukkit.command.Command command, String label, String[] args) {
        if (!sender.isOp()) return true;

        int amount = Integer.parseInt(args[0]);
        int minConnection = Integer.parseInt(args[1]);
        int maxConnection = Integer.parseInt(args[2]);
        int radius = Integer.parseInt(args[3]) * Integer.parseInt(args[3]);

        int chestSpacing = Integer.parseInt(args[4]) * Integer.parseInt(args[4]);

        Player pl = (Player) sender;
        Location l = pl.getLocation();

        Bukkit.getScheduler().runTaskAsynchronously(this, () -> {

            List<Location> possibleChests = floodFill(l, minConnection, maxConnection, radius, pl);

            Bukkit.getScheduler().runTask(this, () -> {
                List<Location> placedCh = new ArrayList<Location>();

                sender.sendMessage("검색 완료, "+radius+"반경 내의 닫힌 공간의 "+possibleChests.size()+" 개의 상자 위치 발견. "+amount+"개 설치 시도중");

                Random r = new Random();
                int placed = 0;
                while (placed < amount) {
                    if (possibleChests.size() == 0) break;

                    int index = r.nextInt(possibleChests.size());
                    Location l2 = possibleChests.get(index);
                    if (!l2.getBlock().isEmpty()) {
                        possibleChests.remove(index);
                        continue;
                    }

                    placedCh.add(l2);
                    l2.getBlock().setType(Material.CHEST);

                    int north = 0, east = 0, west = 0, south= 0;

                    if (l2.getBlock().getRelative(BlockFace.EAST).isEmpty()) {
                        east ++; }
                    if (l2.getBlock().getRelative(BlockFace.WEST).isEmpty()) {
                        west ++; }
                    if (l2.getBlock().getRelative(BlockFace.NORTH).isEmpty()) {
                        north ++; }
                    if (l2.getBlock().getRelative(BlockFace.SOUTH).isEmpty()) {
                        south ++; }

                    if (l2.getBlock().getRelative(BlockFace.NORTH_EAST).isEmpty()) {
                        east ++; north++; }
                    if (l2.getBlock().getRelative(BlockFace.NORTH_WEST).isEmpty()) {
                        west ++; north++; }
                    if (l2.getBlock().getRelative(BlockFace.SOUTH_EAST).isEmpty()) {
                        east ++; south++; }
                    if (l2.getBlock().getRelative(BlockFace.SOUTH_WEST).isEmpty()) {
                        west ++; south++; }

                    if (!l2.getBlock().getRelative(BlockFace.EAST).isEmpty()) {
                        east -= 100; west ++; }
                    if (!l2.getBlock().getRelative(BlockFace.WEST).isEmpty()) {
                        west -= 100; east++;}
                    if (!l2.getBlock().getRelative(BlockFace.NORTH).isEmpty()) {
                        north -= 100; south++; }
                    if (!l2.getBlock().getRelative(BlockFace.SOUTH).isEmpty()) {
                        south -= 100; north++; }

                    if (!l2.getBlock().getRelative(BlockFace.NORTH_EAST).isEmpty()) {
                        west ++; south++; }
                    if (!l2.getBlock().getRelative(BlockFace.NORTH_WEST).isEmpty()) {
                        east ++; south++; }
                    if (!l2.getBlock().getRelative(BlockFace.SOUTH_EAST).isEmpty()) {
                        west ++; north++; }
                    if (!l2.getBlock().getRelative(BlockFace.SOUTH_WEST).isEmpty()) {
                        east ++; north++; }

                    TreeMap<Integer, BlockFace> face = new TreeMap<>((o1, o2) -> Integer.compare(o2, o1));
                    face.put(north, BlockFace.NORTH);
                    face.put(east, BlockFace.EAST);
                    face.put(south, BlockFace.SOUTH);
                    face.put(west, BlockFace.WEST);

                    BlockFace bf = face.values().iterator().next();
                    BlockState bs = l2.getBlock().getState();
                    bs.setData(new Chest(bf));
                    bs.update();






                    possibleChests.removeIf(locc -> locc.distanceSquared(l2) < chestSpacing);

                    placed++;
                }

                UUID uuid=  UUID.randomUUID();
                getDataFolder().mkdirs();
                Path p = getDataFolder().toPath().resolve(uuid.toString()+".txt");

                List<String> str = placedCh.stream().map(l3 -> l3.getBlockX() + ","+l3.getBlockY()+","+l3.getBlockZ()).collect(Collectors.toList());
                try {
                    Files.write(p, str);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                sender.sendMessage(placedCh.size()+" 개의 상자 설치됨. 상자 위치: "+p.toFile().getAbsolutePath());

            });
        });

        return true;
    }

    EnumSet<BlockFace> walls = EnumSet.of(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.SOUTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_WEST);
    EnumSet<BlockFace> directions = EnumSet.of(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH, BlockFace.UP, BlockFace.DOWN);
    EnumSet<BlockFace> directions2 = EnumSet.of(BlockFace.EAST, BlockFace.WEST, BlockFace.SOUTH, BlockFace.NORTH);


    public boolean solidAndNotTransParent(Block b) {
        return !b.isEmpty() && b.getType().isOccluding();
    }

    public boolean check33Platform(Block b) {
        for (BlockFace bf:walls) {
            if (b.getRelative(bf).isEmpty()) return false;
        }
        return true;
    }

    public List<Location> floodFill(Location origin, int faceMin, int faceMax, int radius, Player player) {
        Queue<Location> q = new LinkedList<>();
        q.add(origin);

        List<Location> possibleChests = new ArrayList<>();
        Set<Location> visited = new HashSet<>();

        while (!q.isEmpty()) {
            Location loc = q.poll();
            if (visited.contains(loc)) continue;
            visited.add(loc);

            if (loc.getBlock().isEmpty() && check33Platform(loc.getBlock().getRelative(BlockFace.DOWN)) && solidAndNotTransParent(loc.getBlock().getRelative(BlockFace.DOWN)) && !loc.getBlock().getRelative(BlockFace.UP).getType().isSolid()) {
                int rel = 0;
                boolean ay = false;
                for (BlockFace f:directions2) {
                    if (door.contains(loc.getBlock().getRelative(f).getType())) {
                        ay = true;
                        break;
                    }
                }
                if (!ay) {
                    for (BlockFace f:walls) {
                        if (solidAndNotTransParent(loc.getBlock().getRelative(f)))
                            rel ++;
                    }

                    if (faceMin <= rel && rel <= faceMax) {
                        possibleChests.add(loc);
                    }
                }
            }


            if (loc.getBlock().isEmpty() || door.contains(loc.getBlock().getType())) {
                for (BlockFace bf:directions) {
                    Block b = loc.getBlock().getRelative(bf);
                    if (!b.isEmpty() && !door.contains(b.getType())) continue;
                    if (b.getLocation().distanceSquared(origin) > radius) continue;
                    q.add(b.getLocation());
                }
            }

            if (visited.size() % 20000 == 0) {
                player.sendMessage("검색중... "+visited.size()+"개 검색됨, "+q.size()+"개 더 검색해야함. "+ possibleChests.size()+"개 가능");
                System.out.println("검색중... "+visited.size()+"개 검색됨, "+q.size()+"개 더 검색해야함. "+ possibleChests.size()+"개 가능");
            }
        }

        return possibleChests;
    }
    EnumSet<Material> door = EnumSet.of(Material.ACACIA_DOOR, Material.BIRCH_DOOR, Material.DARK_OAK_DOOR, Material.IRON_DOOR, Material.JUNGLE_DOOR, Material.SPRUCE_DOOR, Material.WOOD_DOOR, Material.WOODEN_DOOR);

}
