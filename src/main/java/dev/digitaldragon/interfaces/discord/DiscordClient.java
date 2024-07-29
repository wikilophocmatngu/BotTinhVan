package dev.digitaldragon.interfaces.discord;

import dev.digitaldragon.WikiBot;
import dev.digitaldragon.jobs.Job;
import dev.digitaldragon.jobs.JobMeta;
import dev.digitaldragon.jobs.JobStatus;
import dev.digitaldragon.util.Config;
import lombok.Getter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Emoji;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.ItemComponent;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class DiscordClient {
    @Getter
    private JDA instance;
    @Getter
    private DiscordJobListener jobListener;

    public DiscordClient() {
        Logger logger = LoggerFactory.getLogger(DiscordClient.class);
        Config.DiscordConfig config = WikiBot.getConfig().getDiscordConfig();
        if (!config.isEnabled()) {
            logger.info("Discord module is disabled, skipping...");
            return;
        }
        enabled = true;

        GatewayIntent[] INTENTS = { GatewayIntent.DIRECT_MESSAGES,GatewayIntent.GUILD_MESSAGES, GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES,GatewayIntent.GUILD_MEMBERS, GatewayIntent.GUILD_PRESENCES, GatewayIntent.GUILD_EMOJIS };
        try {
            instance = JDABuilder.create(config.token(), Arrays.asList(INTENTS))
                    .enableCache(CacheFlag.VOICE_STATE)
                    .setStatus(OnlineStatus.DO_NOT_DISTURB)
                    //.addEventListeners(new DokuWikiDumperPlugin(), new TestingCommand(), new WikiTeam3Plugin())
                    .addEventListeners(new DiscordDokuWikiListener(), new DiscordMediaWikiListener(), new DiscordAdminListener(), new DiscordReuploadListener(), new DiscordButtonListener(), new DiscordCommandListener())
                    .build();
        } catch (LoginException loginException) {
            instance.shutdownNow();
            logger.error("####################");
            logger.error("Failed to log in to Discord. The Discord module will be disabled.", loginException);
            logger.error("####################");

            enabled = false;
            return;
        }

        instance.updateCommands().addCommands(
                Commands.slash("mediawiki_dump", "Dựng bản kết xuất MediaWiki")
                        // We will only include the following common options here: URL, explain, API, Index, Images, XML, XMLApiExport, XMLRevisions, Delay, Force, BypassCDNImageCompression, and .
                        .addOptions(
                            new OptionData(OptionType.STRING, "url", "Dự án bạn muốn tạo kết xuất")
                                 .addChoice("Wiki Lớp Học Mật Ngữ", "https://lophocmatngu.wiki")
                                 .addChoice("Mật Ngữ Database \(MNDB\)", "https://mndb.lophocmatngu.wiki")
                         )
                        .addOption(OptionType.STRING, "explain", "Ghi chú thêm về tác vụ có thể hiển thị qua /status.", false)
                        .addOption(OptionType.STRING, "api", "Liên kết API của wiki bạn muốn tạo kết xuất", false)
                        .addOption(OptionType.STRING, "index", "Liên kết tới index.php của wiki bạn muốn tạo kết xuất", false)
                        .addOption(OptionType.BOOLEAN, "images", "Tải hình ảnh xuống?", false)
                        .addOption(OptionType.BOOLEAN, "xml", "Tải kết xuất XML?", false)
                        .addOption(OptionType.BOOLEAN, "xmlapiexport", "Tạo XML qua API của MediaWiki", false)
                        .addOption(OptionType.BOOLEAN, "xmlrevisions", "Tạo XML qua API phiên bản của MediaWiki", false)
                        .addOption(OptionType.NUMBER, "delay", "Khoảng chờ giữa mỗi yêu cầu", false)
                        .addOption(OptionType.BOOLEAN, "force", "Bỏ qua yêu cầu thời gian chờ mỗi lần kết xuất?", false)
                        .addOption(OptionType.BOOLEAN, "bypass-cdn-image-compression", "Bỏ qua tính năng nén hình ảnh từ CDN.", false)
                        .addOption(OptionType.STRING, "extra-args", "Thêm biến và giá trị tùy chỉnh vào đây.", false),
                Commands.slash("dokuwiki_dump", "Dump a DokuWiki site with dokuwiki-dumper")
                // Only includes url, explain, auto, ignore-disposition-header-missing
                        .addOption(OptionType.STRING, "url", "The URL of the wiki to dump", false)
                        .addOption(OptionType.STRING, "explain", "Note about job displayed in /status.", false)
                        .addOption(OptionType.BOOLEAN, "auto", "Automatically follow redirects", false)
                        .addOption(OptionType.BOOLEAN, "ignore-disposition-missing", "Ignore missing Content-Disposition headers", false)
                        .addOption(OptionType.STRING, "extra-args", "Passes these command-line arguments in addition to other provided values.", false),
                Commands.slash("pukiwiki_archive", "Archive a PukiWiki site with pukiwiki-dumper")
                // only includes url, auto, ignore-action-disabled-edit, explain
                        .addOption(OptionType.STRING, "url", "The URL of the wiki to dump", false)
                        .addOption(OptionType.BOOLEAN, "auto", "Automatically follow redirects", false)
                        .addOption(OptionType.BOOLEAN, "ignore-action-disabled-edit", "Ignore disabled edit actions", false)
                        .addOption(OptionType.STRING, "explain", "Note about job displayed in /status.", false)
                        .addOption(OptionType.STRING, "extra-args", "Passes these command-line arguments in addition to other provided values.", false),
                Commands.slash("help", "Em cần trợ giúp sao?"),
                Commands.slash("status", "Theo dõi tác vụ tạo kết xuất")
                        .addOption(OptionType.STRING, "job", "ID tác vụ cần theo dõi", true),
                Commands.slash("abort", "Hủy bỏ tác vụ")
                        .addOption(OptionType.STRING, "job", "ID tác vụ muốn hủy bỏ", true)

        ).queue();

        jobListener = new DiscordJobListener();
        WikiBot.getBus().register(jobListener);

    }

    @Getter
    public static boolean enabled = false;

    public static void enable() {
        enabled = true;
        WikiBot.getBus().register(new DiscordJobListener());
    }


    public static EmbedBuilder getStatusEmbed(Job job) {

        EmbedBuilder builder = new EmbedBuilder();
        JobMeta meta = job.getMeta();

        builder.setTitle(meta.getTargetUrl().orElse("Tác vụ"), meta.getTargetUrl().orElse(null));

        builder.addField("Người dùng", meta.getUserName(), true);
        builder.addField("ID tác vụ", "`" +  job.getId() + "`", true);
        builder.addField("Loại", job.getType().name(), true);
        String quickLinks = "";

        if (job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RUNNING) {
            //
        }
        else if (job.getStatus() == JobStatus.FAILED || job.getStatus() == JobStatus.ABORTED) {
            builder.setDescription("<:failed:1214681282626326528> Thất bại");
            builder.setColor(Color.RED);
            builder.addField("Tác vụ thất bại!", String.format("`%s` (Mã thoát `%s`)", job.getRunningTask(), job.getFailedTaskCode()), true);
        }
        else if (job.getStatus() == JobStatus.COMPLETED) {
            builder.setDescription("<:done:1214681284778000504> XONG!");
            builder.setColor(Color.GREEN);
        }

        if (job.getLogsUrl() != null) {
            quickLinks += "[Logs](" + job.getLogsUrl() + ") ";
        }
        if (job.getArchiveUrl() != null) {
            quickLinks += "[Archive](" + job.getArchiveUrl() + ") ";
        }

        if (!quickLinks.isEmpty()) {
            builder.addField("Liên kết nhanh", quickLinks, true);
        }


        switch (job.getStatus()) {
            case QUEUED:
                builder.setDescription("<:inprogress:1214681283771375706> Chờ thực thi...");
                builder.setColor(Color.YELLOW);
                break;
            case RUNNING:
                builder.setDescription("<:inprogress:1214681283771375706> Đang thực thi...");
                builder.setColor(Color.YELLOW);
                builder.addField("Task", job.getRunningTask() == null ? "Không rõ" : job.getRunningTask(), true);
                break;
            case FAILED:
                builder.setDescription("<:failed:1214681282626326528> Thất bại");
                builder.setColor(Color.RED);
                if (job.getFailedTaskCode() == 88) {
                    builder.setDescription("<:inprogress:1214681283771375706> Đã bị hủy bỏ\n\n**Công việc này đã tự động bị hủy bỏ vì wiki này đã tạo bản kết xuất cuối chưa đầy một năm trước!**\nChạy lệnh thực thi một lần nữa với giá trị `Force` là `true` nếu vẫn muốn thực thi tác vụ này.");
                    builder.setColor(Color.YELLOW);
                }
                break;
            case ABORTED:
                builder.setDescription("<:failed:1214681282626326528> Đã hủy bỏ");
                builder.setColor(Color.ORANGE);
                break;
            case COMPLETED:
                builder.setDescription("<:done:1214681284778000504> XONG!");
                builder.setColor(Color.GREEN);
                break;
        }
        if (meta.getExplain().isPresent()) {
            builder.addField("Giải thích", meta.getExplain().get(), false);
        }
        return builder;
    }


    public static List<ItemComponent> getJobActionRow(Job job) {
        return List.of(getAbortButton(job), getStatusButton(job), getLogsButton(job), getArchiveButton(job));
    }

    public static Button getAbortButton(Job job) {
        return Button.danger("abort_" + job.getId(), "Hủy bỏ")
                .withEmoji(Emoji.fromUnicode("✖"))
                .withDisabled(!(job.getStatus() == JobStatus.QUEUED || job.getStatus() == JobStatus.RUNNING));
    }

    public static Button getStatusButton(Job job) {
        return Button.secondary("status_" + job.getId(), "Chi tiết")
                .withEmoji(Emoji.fromUnicode("ℹ️"));
    }

    public static Button getLogsButton(Job job) {
        if (job.getLogsUrl() == null) {
            return Button.secondary("logs_" + job.getId(), "Nhật trình")
                    //.withUrl("about:blank")
                    .withEmoji(Emoji.fromUnicode("📄"))
                    .withDisabled(true);
        }
        return Button.secondary("logs_" + job.getId(), "Nhật trình")
                .withEmoji(Emoji.fromUnicode("📄"))
                .withUrl(job.getLogsUrl());
    }

    public static Button getArchiveButton(Job job) {
        if (job.getArchiveUrl() == null) {
            return Button.secondary("archive_" + job.getId(), "Bản lưu")
                    //.withUrl("about:blank")
                    .withEmoji(Emoji.fromUnicode("📁"))
                    .withDisabled(true);
        }
        return Button.secondary("archive_" + job.getId(), "Bản lưu")
                .withEmoji(Emoji.fromUnicode("📁"))
                .withUrl(job.getArchiveUrl());
    }

    public Optional<User> getUserById(String id) {
        //loop through all guilds to check for the user
        for (var guild : instance.getGuilds()) {
            try {
                User user = guild.retrieveMemberById(id).complete().getUser();
                if (user != null) {
                    return Optional.of(user);
                }
            } catch (RuntimeException e) {
                LoggerFactory.getLogger(DiscordClient.class).error("Không tra được thông tin người dùng qua ID", e);
            }
        }
        return Optional.empty();
    }




}
