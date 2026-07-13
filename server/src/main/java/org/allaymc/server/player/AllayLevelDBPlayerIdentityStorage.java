package org.allaymc.server.player;

import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.player.PlayerIdentityStorage;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

/**
 * A {@link PlayerIdentityStorage} implementation backed by LevelDB. Two key namespaces are used:
 * {@code xuid:<xuid>} which maps to the player's last known name, and {@code name:<lowercased name>}
 * which maps back to the player's xuid.
 */
@Slf4j
public class AllayLevelDBPlayerIdentityStorage implements PlayerIdentityStorage {

    protected static final String KEY_PREFIX_XUID = "xuid:";
    protected static final String KEY_PREFIX_NAME = "name:";

    protected final DB db;

    public AllayLevelDBPlayerIdentityStorage(Path folderPath) {
        try {
            this.db = Iq80DBFactory.factory.open(folderPath.toFile(), new Options().createIfMissing(true));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to open player identity storage: " + folderPath, e);
        }
    }

    @Override
    public synchronized void rememberIdentity(String xuid, String name) {
        var oldNameBytes = db.get(xuidKey(xuid));
        if (oldNameBytes != null) {
            var oldName = new String(oldNameBytes, StandardCharsets.UTF_8);
            if (!oldName.equalsIgnoreCase(name)) {
                // Only delete the old name mapping if it still points to this player,
                // as the old name may have been taken over by another player
                var oldNameOwner = db.get(nameKey(oldName));
                if (oldNameOwner != null && xuid.equals(new String(oldNameOwner, StandardCharsets.UTF_8))) {
                    db.delete(nameKey(oldName));
                }
            }
        }

        db.put(xuidKey(xuid), name.getBytes(StandardCharsets.UTF_8));
        db.put(nameKey(name), xuid.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public synchronized Optional<String> lookupXuidByName(String name) {
        return read(nameKey(name));
    }

    @Override
    public synchronized Optional<String> lookupNameByXuid(String xuid) {
        return read(xuidKey(xuid));
    }

    public synchronized void shutdown() {
        try {
            db.close();
        } catch (IOException e) {
            log.error("Error while closing player identity storage", e);
        }
    }

    protected Optional<String> read(byte[] key) {
        var value = db.get(key);
        return value == null ? Optional.empty() : Optional.of(new String(value, StandardCharsets.UTF_8));
    }

    protected static byte[] xuidKey(String xuid) {
        return (KEY_PREFIX_XUID + xuid).getBytes(StandardCharsets.UTF_8);
    }

    protected static byte[] nameKey(String name) {
        return (KEY_PREFIX_NAME + name.toLowerCase(Locale.ROOT)).getBytes(StandardCharsets.UTF_8);
    }
}
