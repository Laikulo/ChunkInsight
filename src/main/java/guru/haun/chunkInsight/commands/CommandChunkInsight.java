package guru.haun.chunkInsight.commands;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * Created by KJ4IPS on 4/6/2016.
 */
public class CommandChunkInsight extends CommandBase {
    @Override
    public String getCommandName() {
        return "cinsight";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        //x,y,dim are only optional for things with positions.
        return sender instanceof Entity ? "/cinsight [x y], [dim]" : "/cinsight <x> <y> <dim>";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) throws CommandException {
        //Valid variations
        // /cinsight                -- Entity only, uses entity's positions and dimension
        // /cinsight <x> <y>        -- Entity only, uses entity's dimension
        // /cinsight <x> <y> <dim>  -- All Senders, uses given  dimension
        int x,z,dim;
        if(args.length == 3){
            x = parseInt(args[0]);
            z = parseInt(args[1]);
            dim = parseInt(args[2]);
        } else if(args.length == 2 || args.length == 0) {
            if(sender instanceof Entity){
                dim = ((Entity) sender).dimension;
                if(args.length == 0) {
                    x = ((Entity) sender).chunkCoordX;
                    z = ((Entity) sender).chunkCoordZ;
                } else {
                    x = parseInt(args[0]);
                    z = parseInt(args[1]);
                }
            }else{
                throw new WrongUsageException("This requires all args when used by a non-player");
            }
        } else {
            throw new SyntaxErrorException();
        }

        boolean shouldBeLoaded = false; //Track if the chunk should be loaded

        if(!DimensionManager.isDimensionRegistered(dim)){
            throw new WrongUsageException(String.format("Dimension %d does not exist or is not loaded", dim));
        }

        WorldServer worldServer = DimensionManager.getWorld(dim);

        if(!worldServer.theChunkProviderServer.chunkExists(x,z)){
            throw new WrongUsageException(String.format("Chunk %d,%d in dimension %d is not loaded", x,z,dim));
        }

        sender.addChatMessage(
                new ChatComponentText(
                        String.format("Chunk Statistics for %d,%d in dimension %d(%s)",x,z,dim,
                                worldServer.getWorldInfo().getWorldName())
                )
        );

        //Players loading the chunk
        if(worldServer.getPlayerManager().hasPlayerInstance(x,z)) {
            //Chunk has a player instance, and therefore is loaded by a player
            shouldBeLoaded = true;

            Class playerInstanceClass = PlayerManager.class.getDeclaredClasses()[0]; //This is PlayerInstance
            LongHashMap<?> playerInstances;

            try {
                playerInstances = ObfuscationReflectionHelper.getPrivateValue(
                        PlayerManager.class,
                        worldServer.getPlayerManager(),
                        "playerInstances", "field_72700_c");
            } catch (RuntimeException  e) {
                e.printStackTrace();
                throw new CommandException("Could not get PlayerInstance from PlayerManager");
            }

            Object playerInstance = playerInstances.getValueByKey(
                    (long) x + 2147483647L | (long) z + 2147483647L << 32);

            Field piPlayersWatchingChunk;
            try {
                piPlayersWatchingChunk = ReflectionHelper.findField(playerInstanceClass,
                        ObfuscationReflectionHelper.remapFieldNames("net.minecraft.server.management.PlayerManager$PlayerInstance",
                                "playersWatchingChunk", "field_73263_b")
                );
            } catch (RuntimeException e) {
                e.printStackTrace();
                throw new CommandException("Could not get PlayersWatchingChunk field from PlayerInstance");
            }

            List<EntityPlayerMP> playersWatchingChunk;
            try {
                piPlayersWatchingChunk.setAccessible(true);
                playersWatchingChunk = (List<EntityPlayerMP>) piPlayersWatchingChunk.get(playerInstance);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
                throw new CommandException("Could not get PlayersWatchingChunk value from PlayerInstance");
            }

            for (EntityPlayerMP player : playersWatchingChunk) {
                sender.addChatMessage(
                        new ChatComponentText("Watched By: ").appendSibling(
                                player.getDisplayName())
                );
            }
        }else{
            sender.addChatMessage(new ChatComponentText("No Players"));
        }

        //Forge's ticketing system
        ImmutableSetMultimap<ChunkCoordIntPair, ForgeChunkManager.Ticket> worldChunks =
                ForgeChunkManager.getPersistentChunksFor(worldServer);

        ImmutableSet<ForgeChunkManager.Ticket> thisChunkTickets = worldChunks.get(new ChunkCoordIntPair(x, z));

        if(thisChunkTickets.isEmpty()){
            sender.addChatMessage(new ChatComponentText("No Tickets"));
        }else{
            //A Forge ticket exists, this causes loading
            shouldBeLoaded = true;
            sender.addChatMessage(new ChatComponentText("Tickets:"));
            for(ForgeChunkManager.Ticket ticket : thisChunkTickets){
                IChatComponent icc  = new ChatComponentText("");
                if(ticket.isPlayerTicket()) {
                    icc.appendSibling(
                            new ChatComponentText(String.format(" Player: %s", ticket.getPlayerName()))
                    );
                }else if(ticket.getType().equals(ForgeChunkManager.Type.ENTITY)){
                    icc.appendText(String.format(" Entity: %s",ticket.getEntity().getName()));
                }else{
                    icc.appendText(" SERVER");
                }

                icc.appendText(String.format(" Mod: %s", ticket.getModId()));
                sender.addChatMessage(icc);
            }
        }

        //Drop queue status
        Set<Long> dropQueue = ObfuscationReflectionHelper.getPrivateValue(ChunkProviderServer.class
                ,worldServer.theChunkProviderServer,"droppedChunksSet", "field_73248_b");
        if(dropQueue.contains(ChunkCoordIntPair.chunkXZ2Int(x,z))){
            sender.addChatMessage(new ChatComponentText("Chunk is in drop queue"));
        }

        if(worldServer.isSpawnChunk(x,z)){
            //Spawn Chunks should always be loaded
            sender.addChatMessage(new ChatComponentText("Spawn chunk"));
            shouldBeLoaded = true;
        }

        //Whine if chunk is orphaned
        if(!shouldBeLoaded){
            sender.addChatMessage(new ChatComponentText("Chunk should not be loaded"));
        }

    }
}
