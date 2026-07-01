package ctx.mr_hares.flashWhite.utils;

import net.dv8tion.jda.api.entities.emoji.Emoji;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EmojiParser {

    private static final Pattern CUSTOM_EMOJI_PATTERN =
            Pattern.compile("<(a?):(\\w+):(\\d+)>");

    public static Emoji parseEmoji(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            return null;
        }

        Matcher matcher = CUSTOM_EMOJI_PATTERN.matcher(symbol);

        if (matcher.matches()) {
            boolean isAnimated = matcher.group(1).equals("a");
            String name = matcher.group(2);
            long id = Long.parseLong(matcher.group(3));

            return Emoji.fromCustom(name, id, isAnimated);
        } else {
            return Emoji.fromUnicode(symbol);
        }
    }
}