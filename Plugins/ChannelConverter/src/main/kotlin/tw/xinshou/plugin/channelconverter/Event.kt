package tw.xinshou.plugin.channelconverter

import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.Webhook
import net.dv8tion.jda.api.utils.FileUpload
import tw.xinshou.loader.base.BotLoader.jdaBot
import tw.xinshou.loader.plugin.PluginEvent


object Event : PluginEvent(true) {
    // NTUST CSIE
    val guild by lazy { jdaBot.getGuildById(1373282474439999558L)!! }
    val sourceForumChannel by lazy { guild.getForumChannelById(1373298780228816916L)!! }
//    val targetGuild by lazy { jdaBot.getGuildById(1373282474439999558L)!! }
//    val targetCategory by lazy { targetGuild.getCategoryById(1408463187799707788L)!! }

    // DEV
    //    val guild by lazy { jdaBot.getGuildById(1267003970757591081L)!! }
    //    val sourceForumChannel by lazy { guild.getForumChannelById(1267004922029805568L)!! }
    val targetGuild by lazy { jdaBot.getGuildById(1267003970757591081L)!! }
    val targetCategory by lazy { targetGuild.getCategoryById(1408522687231426692L)!! }
    val webhooks = mutableListOf<Webhook>()

    override fun unload() {
        super.unload()
        webhooks.forEach {
            it.delete().complete()
        }
    }

    override fun load() {
        super.load()
        logger.info("Starting to convert forum channels...")
//        targetCategory.channels.forEach {
//            it.delete().complete()
//        }
//        return
        sourceForumChannel.threadChannels.forEach { channel ->
            logger.info("Converting channel: ${channel.name}")
            targetCategory.createTextChannel(channel.name).queue { newChannel ->
                newChannel.createWebhook("ChannelConverter").queue { webhook ->
                    webhooks.add(webhook)
                    logger.info("Created webhook for channel: ${channel.name}")
                    channel.iterableHistory.queue { msg ->
                        msg.reversed().forEach { revMsg ->
                            sendMessage(webhook, revMsg)
                        }
                        logger.info("Completed converting channel: ${channel.name}")
                    }
                }
            }
        }
        logger.info("Finished converting all forum channels")
    }

    fun sendMessage(webhook: Webhook, message: Message) {
        try {
            if (message.attachments.isNotEmpty()) {
                logger.debug("Sending message with ${message.attachments.size} attachments")
                try {
                    webhook.sendMessage(message.contentRaw)
                        .setFiles(message.attachments.map { it ->
                            FileUpload.fromData(it.proxy.download().get(), it.fileName)
                        })
                        .setUsername(message.member!!.effectiveName)
                        .setAvatarUrl(message.author.effectiveAvatarUrl)
                        .setAllowedMentions(listOf())
                        .queue()
                } catch (e: Exception) {
                    logger.error("Failed to send message with attachments: ${e.message}")
                    logger.error("Channel ID: ${message.channel.id}, Channel Name: ${message.channel.name}")
                    logger.error("Author: ${message.author.name}")
                    logger.error("Attachments: ${message.attachments.joinToString { it.fileName }}")

                    // fallback
                    if (message.contentRaw.isNotEmpty()) {
                        webhook.sendMessage(message.contentRaw)
                            .setUsername(message.member!!.effectiveName)
                            .setAvatarUrl(message.author.effectiveAvatarUrl)
                            .setAllowedMentions(listOf())
                            .queue()
                    } else {
                        logger.warn("Message content is empty, skipping fallback message")
                    }
                }
            } else {
                logger.debug("Sending text message")
                webhook.sendMessage(message.contentRaw)
                    .setUsername(message.member!!.effectiveName)
                    .setAvatarUrl(message.author.effectiveAvatarUrl)
                    .setAllowedMentions(listOf())
                    .queue()
            }
        } catch (e: Exception) {
            logger.error("Failed to send message: ${e.message}")
            e.printStackTrace()
        }
    }
}