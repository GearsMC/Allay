package org.allaymc.server.player;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.player.PlayerData;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * @author daoge_cmd
 */
@Slf4j
public class AllayNBTFilePlayerStorage extends AllayPlayerStorage {
    protected Path dataFolderPath;

    @SneakyThrows
    public AllayNBTFilePlayerStorage(Path dataFolderPath) {
        this.dataFolderPath = dataFolderPath;
        if (!Files.exists(dataFolderPath)) Files.createDirectory(dataFolderPath);
    }

    @Override
    public PlayerData readPlayerData(String xuid) {
        var path = buildPlayerDataFilePath(xuid);
        if (!Files.exists(path)) return PlayerData.createEmpty();

        try (var reader = NbtUtils.createGZIPReader(Files.newInputStream(path))) {
            return PlayerData.fromNBT((NbtMap) reader.readTag());
        } catch (Throwable e) {
            log.error("Error while reading player data {}", xuid, e);
            return PlayerData.createEmpty();
        }
    }

    @SneakyThrows
    @Override
    public void savePlayerData(String xuid, PlayerData playerData) {
        var path = buildPlayerDataFilePath(xuid);

        var oldPath = path.resolveSibling(xuid + "_old.nbt");
        if (Files.exists(oldPath)) {
            // The old file
            log.warn("Undeleted tmp player data file is found, which may caused by incorrect shutdown. File: {}", oldPath);
            Files.delete(oldPath);
        }

        // Rename current file to xuid_old.nbt
        var currentFileExists = Files.exists(path);
        if (currentFileExists) Files.move(path, oldPath);

        try (var writer = NbtUtils.createGZIPWriter(Files.newOutputStream(path))) {
            writer.writeTag(playerData.toNBT());
        } catch (Throwable e) {
            if (currentFileExists) {
                // error, rename xuid_old.nbt file to xuid.nbt
                Files.move(oldPath, path);
            }
            log.error("Error while writing player data {}", xuid, e);
        }

        // delete xuid_old.nbt file
        Files.deleteIfExists(oldPath);
    }

    @SneakyThrows
    @Override
    public boolean removePlayerData(String xuid) {
        return Files.deleteIfExists(buildPlayerDataFilePath(xuid));
    }

    @Override
    public boolean hasPlayerData(String xuid) {
        return Files.exists(buildPlayerDataFilePath(xuid));
    }

    protected Path buildPlayerDataFilePath(String xuid) {
        return dataFolderPath.resolve(xuid + ".nbt");
    }
}
