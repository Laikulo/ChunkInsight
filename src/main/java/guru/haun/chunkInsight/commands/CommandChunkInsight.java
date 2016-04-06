package guru.haun.chunkInsight.commands;

import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.PlayerManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.LongHashMap;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;

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
        int x,y,dim;
        if(args.length == 3){
            x = parseInt(args[0]);
            y = parseInt(args[1]);
            dim = parseInt(args[2]);
        } else if(args.length == 2 || args.length == 0) {
            if(sender instanceof Entity){
                dim = ((Entity) sender).dimension;
                if(args.length == 0) {
                    x = ((Entity) sender).chunkCoordX;
                    y = ((Entity) sender).chunkCoordZ;
                } else {
                    x = parseInt(args[0]);
                    y = parseInt(args[1]);
                }
            }else{
                throw new WrongUsageException("This requires all args when used by a non-player");
            }
        } else {
            throw new SyntaxErrorException();
        }
        WorldServer worldServer = DimensionManager.getWorld(dim);
        sender.addChatMessage(
                new ChatComponentText(
                        String.format("Chunk Statistics for %d,%d in dimension %d(%s)",x,y,dim,
                                worldServer.getWorldInfo().getWorldName())
                )
        );

        if(worldServer.getPlayerManager().hasPlayerInstance(x,y)) {

            Class playerInstanceClass = PlayerManager.class.getDeclaredClasses()[0]; //This is PlayerInstance
            LongHashMap<?> playerInstances = ObfuscationReflectionHelper.getPrivateValue(PlayerManager.class,
                    worldServer.getPlayerManager(), "playerInstances");

            Object playerInstance = playerInstances.getValueByKey(
                    (long) x + 2147483647L | (long) y + 2147483647L << 32);

            Field piPlayersWatchingChunk;
            try {
                piPlayersWatchingChunk = playerInstanceClass.getDeclaredField(
                        ObfuscationReflectionHelper.remapFieldNames("PlayerManager$PlayerInstance",
                                "playersWatchingChunk")[0]
                );
            } catch (NoSuchFieldException e) {
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
            sender.addChatMessage(new ChatComponentText("Chunk is not watched by any players"));
        }

    }
}
