package org.allaymc.server.command.defaults;

import com.sun.jna.platform.win32.COM.WbemcliUtil;
import lombok.extern.slf4j.Slf4j;
import org.allaymc.api.command.Command;
import org.allaymc.api.command.CommandSender;
import org.allaymc.api.command.tree.CommandTree;
import org.allaymc.api.message.I18n;
import org.allaymc.api.message.TrKeys;
import org.allaymc.api.permission.Permissions;
import org.allaymc.api.server.Server;
import org.allaymc.api.utils.TextFormat;
import org.allaymc.api.world.Dimension;
import org.allaymc.server.AllayServer;
import org.allaymc.server.player.ChunkCache;
import oshi.SystemInfo;
import oshi.util.platform.windows.WmiQueryHandler;

import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.allaymc.api.math.MathUtils.round;

/**
 * @author daoge_cmd
 */
@Slf4j
public class StatusCommand extends Command {

    protected static final Map<String, String> VM_VENDOR = new HashMap<>(10, 0.99f);
    protected static final Map<String, String> VM_MAC = new HashMap<>(10, 0.99f);
    protected static final String[] VM_MODEL_ARRAY = new String[]{
            "Linux KVM", "Linux lguest", "OpenVZ", "Qemu",
            "Microsoft Virtual PC", "VMWare", "linux-vserver",
            "Xen", "FreeBSD Jail", "VirtualBox", "Parallels",
            "Linux Containers", "LXC", "Bochs"
    };

    protected static final SystemInfo SYSTEM_INFO = new SystemInfo();

    private static final double WARN_USAGE = 80;
    private static final double CRIT_USAGE = 90;

    static {
        // VM VENDOR
        VM_VENDOR.put("bhyve", "bhyve");
        VM_VENDOR.put("KVM", "KVM");
        VM_VENDOR.put("TCG", "QEMU");
        VM_VENDOR.put("Microsoft Hv", "Microsoft Hyper-V or Windows Virtual PC");
        VM_VENDOR.put("lrpepyh vr", "Parallels");
        VM_VENDOR.put("VMware", "VMware");
        VM_VENDOR.put("XenVM", "Xen HVM");
        VM_VENDOR.put("ACRN", "Project ACRN");
        VM_VENDOR.put("QNXQVMBSQG", "QNX Hypervisor");
        // VM MAC
        VM_MAC.put("00:50:56", "VMware ESX 3");
        VM_MAC.put("00:0C:29", "VMware ESX 3");
        VM_MAC.put("00:05:69", "VMware ESX 3");
        VM_MAC.put("00:03:FF", "Microsoft Hyper-V");
        VM_MAC.put("00:1C:42", "Parallels Desktop");
        VM_MAC.put("00:0F:4B", "Virtual Iron 4");
        VM_MAC.put("00:16:3E", "Xen or Oracle VM");
        VM_MAC.put("08:00:27", "VirtualBox");
        VM_MAC.put("02:42:AC", "Docker Container");
    }

    public StatusCommand() {
        // GearsMC: bir sure "allaystatus" adini tasidi, cunku GearsCore /durum'a "status" diye Ingilizce
        // ikiz kaydediyordu ve eklenti motordan sonra yuklendigi icin bu komutu eziyordu. Cikti Turkcelestirilip
        // zenginlestirilince (2026-09-21) "status" motora geri verildi; GearsCore'da /durum tek basina kaldi.
        super("status", TrKeys.ALLAY_COMMAND_STATUS_DESCRIPTION, Permissions.COMMAND_STATUS);
    }

    protected static void printOperationSystemMemoryInfo(CommandSender sender) {
        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_MEMORYINFO_HEADER);
        var globalMemory = SYSTEM_INFO.getHardware().getMemory();
        var virtualMemory = globalMemory.getVirtualMemory();

        // Physical Memory
        var totalPhys = globalMemory.getTotal();
        var usedPhys = totalPhys - globalMemory.getAvailable();
        sendMemoryUsage(sender, TrKeys.ALLAY_COMMAND_STATUS_MEMORY_PHYSICAL, usedPhys, totalPhys);

        // Virtual Memory
        var totalVirt = virtualMemory.getVirtualMax();
        var usedVirt = virtualMemory.getVirtualInUse();
        // Some hosts report 0 virtual max; avoid div/0
        if (totalVirt > 0) {
            sendMemoryUsage(sender, TrKeys.ALLAY_COMMAND_STATUS_MEMORY_VIRTUAL, usedVirt, totalVirt);
        } else {
            sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_MEMORY_UNAVAILABLE,
                    I18n.get().tr(TrKeys.ALLAY_COMMAND_STATUS_MEMORY_VIRTUAL));
        }

        // Hardware
        var physicalMemories = globalMemory.getPhysicalMemory();
        if (!physicalMemories.isEmpty()) {
            sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_HARDWARE_HEADER);
            for (var each : physicalMemories) {
                sender.sendMessage("- " + TextFormat.GREEN + each.getBankLabel() + " " + formatFreq(each.getClockSpeed())
                                   + TextFormat.WHITE + " " + toMB(each.getCapacity()));
                sender.sendMessage("  " + TextFormat.GREEN + each.getMemoryType() + ", " + each.getManufacturer());
            }
        }
        sender.sendMessage("\n");
    }

    protected static void printCPUInfo(CommandSender sender) {
        var cpu = SYSTEM_INFO.getHardware().getProcessor();
        var processorIdentifier = cpu.getProcessorIdentifier();

        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_CPUINFO_HEADER);
        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_CPU_MODEL,
                TextFormat.GREEN + processorIdentifier.getName().trim(), formatFreq(cpu.getMaxFreq()));
        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_CPU_FEATURES,
                processorIdentifier.isCpu64bit() ? "64bit" : "32bit",
                processorIdentifier.getModel(), processorIdentifier.getMicroarchitecture());
        sender.sendMessage("\n");
    }

    protected static void printNetworkInfo(CommandSender sender) {
        try {
            var networkIFs = SYSTEM_INFO.getHardware().getNetworkIFs();
            if (networkIFs == null || networkIFs.isEmpty()) {
                return;
            }

            sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_NETWORKINFO_HEADER);
            for (var nic : networkIFs) {
                var addresses = Stream.concat(
                        Arrays.stream(nic.getIPv4addr()),
                        Arrays.stream(nic.getIPv6addr())
                ).toList();

                String speedStr = nic.getSpeed() > 0 ? toKB(nic.getSpeed()) + "/s " : "";
                sender.sendMessage(
                        "- " + TextFormat.GREEN + nic.getDisplayName() + " " + speedStr
                        + TextFormat.YELLOW + String.join(", ", addresses)
                );
            }
            sender.sendMessage("\n");
        } catch (Exception e) {
            sender.sendTranslatable(TextFormat.RED + I18n.get().tr(TrKeys.ALLAY_COMMAND_STATUS_NETWORK_FAILED));
            log.debug("Network info retrieval failed", e);
        }
    }

    protected static void printOperationSystemAndJVMInfo(CommandSender sender) {
        var os = SYSTEM_INFO.getOperatingSystem();
        var mxBean = ManagementFactory.getRuntimeMXBean();

        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_OSINFO_HEADER);
        var versionInfo = os.getVersionInfo();
        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_OS,
                TextFormat.GREEN + os.getFamily(), os.getManufacturer(), versionInfo.getVersion(),
                String.valueOf(versionInfo.getCodeName()), String.valueOf(os.getBitness()),
                String.valueOf(versionInfo.getBuildNumber()));
        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_JVM,
                TextFormat.GREEN + mxBean.getVmName(), mxBean.getVmVendor(), mxBean.getVmVersion());
        try {
            String vm = detectVM();
            sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_VM, TextFormat.GREEN + (vm == null ? "N/A" : vm));
        } catch (Exception e) {
            sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_VM, TextFormat.GREEN + "N/A");
            log.debug("VM detection error", e);
        }
        sender.sendMessage("\n");
    }

    /**
     * GearsMC eki: istemci chunk onbelleginin durumu.
     *
     * <p>Onbellek varsayilan acik oldugu icin gercekten ise yarayip yaramadigini gorebilmek gerekiyor; isabet
     * orani dusukse {@code max-chunk-cache-blobs} kucuk demektir (LRU blob'lari erken atiyor) ya da oyuncular
     * ayni bolgeleri hic paylasmiyordur.</p>
     */
    protected static void printChunkCacheInfo(CommandSender sender) {
        var settings = AllayServer.getSettings().networkSettings();
        if (!settings.enableClientChunkCache()) {
            sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_CHUNKCACHE_DISABLED);
            return;
        }

        var stats = ChunkCache.getInstance().getStats();
        var caffeine = stats.caffeineStats();
        var requests = caffeine.requestCount();
        var hitRate = requests == 0 ? 0d : caffeine.hitRate() * 100d;
        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_CHUNKCACHE,
                TextFormat.GREEN + String.valueOf(stats.blobCount()), String.valueOf(settings.maxChunkCacheBlobs()),
                String.valueOf(stats.playerCount()), String.format(Locale.ROOT, "%.1f", stats.clientHitRate()),
                String.valueOf(stats.advertisedBlobs()), String.format(Locale.ROOT, "%.1f", hitRate),
                String.valueOf(caffeine.evictionCount()));
    }

    /**
     * GearsMC eki: kisa ciktida da islemci ozeti. Cekirdek sayisi ve yuk, tik suresinin neden uzadigini anlamak
     * icin gerekli; "full" argumaniyla gelen tam donanim dokumunu beklemeye gerek kalmasin.
     */
    protected static void printCpuSummary(CommandSender sender) {
        var cpu = SYSTEM_INFO.getHardware().getProcessor();
        // Bir dakikalik yuk ortalamasi; Linux'ta hazir, diger sistemlerde -1 gelebilir.
        var load = cpu.getSystemLoadAverage(1)[0];
        var loadPct = load < 0 ? 0d : load * 100d / Math.max(cpu.getLogicalProcessorCount(), 1);
        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_CPU,
                TextFormat.GREEN + String.valueOf(cpu.getPhysicalProcessorCount()),
                String.valueOf(cpu.getLogicalProcessorCount()),
                load < 0 ? "?" : String.format(Locale.ROOT, "%.1f", loadPct));
        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_THREADS,
                TextFormat.GREEN + String.valueOf(Thread.getAllStackTraces().size()));
    }

    protected static void printWorldInfo(CommandSender sender) {
        var worlds = Server.getInstance().getWorldPool().getWorlds().values();
        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_WORLDS_HEADER, TextFormat.GREEN + String.valueOf(worlds.size()));

        var totalChunks = 0;
        var totalEntities = 0;
        var totalBlockEntities = 0;
        for (var world : worlds) {
            var dims = world.getDimensions().values();
            var chunks = dims.stream().mapToInt(d -> d.getChunkManager().getLoadedChunks().size()).sum();
            var entities = dims.stream().mapToInt(Dimension::getEntityCount).sum();
            var blockEntities = dims.stream().mapToInt(Dimension::getBlockEntityCount).sum();
            totalChunks += chunks;
            totalEntities += entities;
            totalBlockEntities += blockEntities;

            var tickUsage = world.getTickUsage() * 100f;
            sender.sendMessage("- " + TextFormat.AQUA + world.getWorldData().getDisplayName());
            sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_WORLD_TICK,
                    colorizeTps(world.getTPS()) + String.format(Locale.ROOT, "%.1f", world.getTPS()),
                    String.format(Locale.ROOT, "%.2f", world.getMSPT()),
                    String.format(Locale.ROOT, "%.1f", tickUsage));
            sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_WORLD_CONTENTS,
                    TextFormat.GREEN + String.valueOf(chunks), String.valueOf(entities), String.valueOf(blockEntities));
        }
        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_TOTALS,
                TextFormat.GREEN + String.valueOf(totalChunks), String.valueOf(totalEntities),
                String.valueOf(totalBlockEntities));
        sender.sendMessage("\n");
    }

    /** Tik hizi dustukce renk koyulasir; 20 tik normal, 15'in altinda sorun var demektir. */
    private static TextFormat colorizeTps(float tps) {
        if (tps < 15f) return TextFormat.RED;
        if (tps < 19f) return TextFormat.GOLD;
        return TextFormat.GREEN;
    }

    protected static void printUpTimeInfo(CommandSender sender) {
        var time = System.currentTimeMillis() - Server.getInstance().getStartTime();
        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_UPTIME, TextFormat.GREEN + formatUptime(time));
    }

    protected static void printMemoryUsageInfo(CommandSender sender) {
        var runtime = Runtime.getRuntime();
        var totalMB = bytesToMB(runtime.totalMemory());
        var usedMB = bytesToMB(runtime.totalMemory() - runtime.freeMemory());
        var maxMB = bytesToMB(runtime.maxMemory());
        var usagePct = maxMB > 0 ? (usedMB / maxMB) * 100d : 0d;

        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_MEMORY_USED,
                colorizeUsage(usagePct) + String.valueOf(round(usedMB, 2)), TextFormat.GREEN + String.valueOf(round(totalMB, 2)),
                String.valueOf(round(maxMB, 2)), String.valueOf(round(usagePct, 2)));
    }

    protected static void printOnlinePlayerInfo(CommandSender sender) {
        var server = Server.getInstance();
        var ps = server.getPlayerManager();

        var online = ps.getPlayerCount();
        var maxPlayerCount = ps.getMaxPlayerCount();
        var ratio = maxPlayerCount > 0 ? (float) online / (float) maxPlayerCount : 0f;

        var color = TextFormat.GREEN;
        if (ratio > 0.90f) {
            color = TextFormat.GOLD;
        } else if (online == maxPlayerCount && maxPlayerCount > 0) {
            color = TextFormat.RED;
        }

        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_PLAYERS, color + String.valueOf(online), String.valueOf(maxPlayerCount));
    }

    protected static String detectVM() {
        var hardware = SYSTEM_INFO.getHardware();

        // CPU model detection
        var vmVendor = VM_VENDOR.get(hardware.getProcessor().getProcessorIdentifier().getVendor().trim());
        if (vmVendor != null) {
            return vmVendor;
        }

        // MAC address detection
        var networkIFs = hardware.getNetworkIFs();
        for (var nif : networkIFs) {
            var mac = nif.getMacaddr().toUpperCase(Locale.ROOT);
            var oui = mac.length() > 7 ? mac.substring(0, 8) : mac;
            var vmMac = VM_MAC.get(oui);
            if (vmMac != null) {
                return vmMac;
            }
        }

        // Model detection
        var model = hardware.getComputerSystem().getModel();
        for (var vm : VM_MODEL_ARRAY) {
            if (model.contains(vm)) {
                return vm;
            }
        }

        var manufacturer = hardware.getComputerSystem().getManufacturer();
        if ("Microsoft Corporation".equals(manufacturer) && "Virtual Machine".equals(model)) {
            return "Microsoft Hyper-V";
        }

        // Memory manufacturer detection
        if ("QEMU".equals(hardware.getMemory().getPhysicalMemory().getFirst().getManufacturer())) {
            return "QEMU";
        }

        // Check Windows system parameters
        // WMI virtual machine query only on Windows
        var osName = System.getProperties().getProperty("os.name", "").toUpperCase(Locale.ROOT);
        if (osName.contains("WINDOWS")) {
            var wmiQuery = new WbemcliUtil.WmiQuery<>("Win32_ComputerSystem", ComputerSystemEntry.class);
            var result = WmiQueryHandler.createInstance().queryWMI(wmiQuery);
            var present = result.getValue(ComputerSystemEntry.HYPERVISORPRESENT, 0);
            if (present != null && "true".equalsIgnoreCase(present.toString())) {
                return "Hyper-V";
            }
        } else {
            // Check for Docker container
            // Docker check only on non-Windows systems
            if (new File("/.dockerenv").exists()) {
                return "Docker Container";
            }

            var cgroupFile = new File("/proc/1/cgroup");
            if (cgroupFile.exists()) {
                try (var lines = Files.lines(cgroupFile.toPath())) {
                    var containerized = lines.anyMatch(line -> line.contains("docker") || line.contains("lxc"));
                    if (containerized) {
                        return "Docker Container";
                    }
                } catch (IOException e) {
                    log.error("Error checking /proc/1/cgroup for containerization", e);
                }
            }
        }

        return null;
    }

    private static void sendMemoryUsage(CommandSender sender, String labelKey, long usedBytes, long totalBytes) {
        var label = I18n.get().tr(labelKey);
        if (totalBytes <= 0) {
            sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_MEMORY_UNAVAILABLE, label);
            return;
        }

        var usagePct = usedBytes * 100d / totalBytes;
        sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_MEMORY_USAGE, label,
                colorizeUsage(usagePct) + toMB(usedBytes), toMB(totalBytes), String.valueOf(round(usagePct, 2)));
    }

    private static TextFormat colorizeUsage(double usagePercent) {
        if (usagePercent > CRIT_USAGE) return TextFormat.RED;
        if (usagePercent > WARN_USAGE) return TextFormat.GOLD;
        return TextFormat.GREEN;
    }

    protected static String toKB(long bytes) {
        return round(bytes / 1024d, 2) + " KB";
    }

    protected static String toMB(long bytes) {
        return round(bytesToMB(bytes), 2) + " MB";
    }

    private static double bytesToMB(long bytes) {
        return bytes / (1024d * 1024d);
    }

    protected static String formatFreq(long hz) {
        if (hz >= 1_000_000_000L) {
            return String.format("%.2fGHz", hz / 1_000_000_000d);
        } else if (hz >= 1_000_000L) {
            return String.format("%.2fMHz", hz / 1_000_000d);
        } else if (hz >= 1_000L) {
            return String.format("%.2fKHz", hz / 1_000d);
        } else if (hz > 0) {
            return hz + "Hz";
        } else {
            return "N/A";
        }
    }

    protected static String formatUptime(long uptime) {
        long days = TimeUnit.MILLISECONDS.toDays(uptime);
        uptime -= TimeUnit.DAYS.toMillis(days);

        long hours = TimeUnit.MILLISECONDS.toHours(uptime);
        uptime -= TimeUnit.HOURS.toMillis(hours);

        long minutes = TimeUnit.MILLISECONDS.toMinutes(uptime);
        uptime -= TimeUnit.MINUTES.toMillis(minutes);

        long seconds = TimeUnit.MILLISECONDS.toSeconds(uptime);
        return I18n.get().tr(TrKeys.ALLAY_COMMAND_STATUS_UPTIME_VALUE,
                String.valueOf(days), String.valueOf(hours), String.valueOf(minutes), String.valueOf(seconds));
    }

    @Override
    public void prepareCommandTree(CommandTree tree) {
        tree.getRoot().bool("full", false).optional().exec(context -> {
            boolean full = context.getResult(0);
            var sender = context.getSender();

            sender.sendTranslatable(TrKeys.ALLAY_COMMAND_STATUS_HEADER);
            printUpTimeInfo(sender);
            printMemoryUsageInfo(sender);
            printOnlinePlayerInfo(sender);
            printCpuSummary(sender);
            printChunkCacheInfo(sender);
            sender.sendMessage("\n");

            printWorldInfo(sender);
            if (full) {
                printOperationSystemAndJVMInfo(sender);
                printNetworkInfo(sender);
                printCPUInfo(sender);
                printOperationSystemMemoryInfo(sender);
            }
            return context.success();
        });
    }

    protected enum ComputerSystemEntry {
        HYPERVISORPRESENT
    }
}