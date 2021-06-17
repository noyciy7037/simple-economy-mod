package com.anatawa12.simpleEconomy;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class MoneyManager extends WorldSavedData {
    public static String identifier = "com.anatawa12.simpleEconomy.MoneyManager";
    private final Map<String, Player> playerByName = new HashMap<>();
    private final Map<String, Player> playerByUUID = new HashMap<>();

    @SuppressWarnings("unused")
    public MoneyManager(String identifier) {
        super(identifier);
    }

    public MoneyManager() {
        super(identifier);
    }

    @Nullable
    public static Player getPlayerByName(String playerName) {
        return getInstance().getPlayerByNameInternal(playerName);
    }

    @Nonnull
    public static Player getPlayerByEntity(EntityPlayer entityPlayer) {
        return getInstance().getPlayerByEntityInternal(entityPlayer);
    }

    @Nullable
    private Player getPlayerByNameInternal(@Nonnull String playerName) {
        EntityPlayerMP entityPlayerMP = MinecraftServer.getServer().getConfigurationManager().func_152612_a(playerName);

        if (entityPlayerMP != null) {
            return getPlayerByEntityInternal(entityPlayerMP);
        } else {
            return playerByName.get(playerName);
        }
    }

    @Nonnull
    private Player getPlayerByEntityInternal(@Nonnull EntityPlayer entityPlayer) {
        if (entityPlayer.worldObj.isRemote)
            throw new IllegalStateException("supported only on server");

        String uuid = getUUID(entityPlayer);
        String playerName = entityPlayer.getDisplayName();

        Player player = playerByUUID.get(uuid);

        if (player == null) player = new Player(uuid);
        player.updateName(playerName);
        return player;
    }

    @Override
    public void readFromNBT(NBTTagCompound compound) {
        NBTTagList tags = compound.getTagList("l", 10);

        for (int i = 0; i < tags.tagCount(); i++) {
            NBTTagCompound playerCompound = tags.getCompoundTagAt(i);
            Player player = new Player(playerCompound.getString("uuid"));
            player.updateName(playerCompound.getString("nane"));
            player.money = playerCompound.getInteger("money");
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound compound) {
        NBTTagList tags = new NBTTagList();

        for (Player player : playerByName.values()) {
            NBTTagCompound playerCompound = new NBTTagCompound();
            playerCompound.setString("uuid", player.uuid);
            playerCompound.setString("name", player.name);
            playerCompound.setInteger("money", player.money);
            tags.appendTag(playerCompound);
        }

        compound.setTag("l", tags);
    }

    public class Player {
        private @Nullable String name;
        public @Nonnull final String uuid;
        public int money;

        private Player(@Nonnull String uuid) {
            if (playerByUUID.containsKey(uuid))
                throw new IllegalStateException("uuid duplicate");
            this.uuid = uuid;

            playerByUUID.put(uuid, this);
        }

        private void updateName(@Nullable String name) {
            if (this.name != null) {
                Player old = playerByName.remove(this.name.toLowerCase(Locale.ROOT));
                if (old != null) old.name = null;
            }
            if (name == null) {
                this.name = null;
                return;
            }
            this.name = name;
            playerByName.put(name.toLowerCase(Locale.ROOT), this);
        }

        @Override
        public String toString() {
            if (name == null)
                return "(no name:" + uuid + ")";
            return name;
        }

        @Nullable
        public Object getName() {
            return name;
        }
    }

    // utils

    @Nonnull
    private static MoneyManager getInstance() {
        World world = MinecraftServer.getServer().getEntityWorld();
        if (world.isRemote)
            throw new IllegalStateException("client side");
        MoneyManager money = (MoneyManager)world.loadItemData(MoneyManager.class, identifier);
        if (money == null) {
            money = new MoneyManager();
            world.setItemData(identifier, money);
        }
        return money;
    }

    @Nonnull
    private static String getUUID(EntityPlayer entityPlayer) {
        UUID uniqueID = entityPlayer.getUniqueID();
        return uniqueID.toString().replace("-", "").toLowerCase(Locale.ROOT);
    }
}