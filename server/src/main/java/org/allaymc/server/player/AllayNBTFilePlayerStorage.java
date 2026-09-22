package org.allaymc.server.player;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.player.PlayerData;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.nbt.NbtUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author daoge_cmd
 */
@Slf4j
public class AllayNBTFilePlayerStorage extends AllayPlayerStorage {
    private static final String DATA_FILE_SUFFIX = ".nbt";
    private static final String OLD_DATA_FILE_SUFFIX = "_old.nbt";

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

    @SneakyThrows
    @Override
    public Set<String> getStoredXuids() {
        var xuids = new HashSet<String>();
        try (var files = Files.list(dataFolderPath)) {
            files.map(file -> file.getFileName().toString())
                    // Yarım kalmış kayıttan kalan "<xuid>_old.nbt" ayrı bir oyuncu değildir.
                    .filter(name -> name.endsWith(DATA_FILE_SUFFIX) && !name.endsWith(OLD_DATA_FILE_SUFFIX))
                    .map(name -> name.substring(0, name.length() - DATA_FILE_SUFFIX.length()))
                    .filter(xuid -> !xuid.isEmpty())
                    .forEach(xuids::add);
        }
        return Collections.unmodifiableSet(xuids);
    }

    protected Path buildPlayerDataFilePath(String xuid) {
        return dataFolderPath.resolve(xuid + ".nbt");
    }
}
