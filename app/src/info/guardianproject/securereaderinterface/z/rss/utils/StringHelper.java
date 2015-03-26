package info.guardianproject.securereaderinterface.z.rss.utils;

import android.content.Context;
import android.text.Html;
import android.text.Spanned;

public class StringHelper {


    public static boolean isEmptyString(String string) {
        if (null == string || 0 >= string.length()) {
            return true;
        }
        return false;
    }

    /**
     * Find tag in CharSequence by current position
     *
     * @param charSequence
     * @return null - if tag not found
     */
    public static String findTagAtPosition(CharSequence charSequence, int currentPosition) {
        int tagStart = -1;
        for (int i = currentPosition; i >= 0; i--) {
            if (charSequence.charAt(i) == ' ') {
                break;
            }
            if (charSequence.charAt(i) == '#') {
                tagStart = i;
                break;
            }
        }
        if (-1 != tagStart) {
            StringBuffer sb = new StringBuffer();
            int i = tagStart + 1;
            while (i < charSequence.length() && (charSequence.charAt(i) != ' ' && charSequence.charAt(i) != '#')) {
                sb.append(charSequence.charAt(i));
                i++;
            }
            return sb.toString();
        } else {
            return null;
        }
    }

    /**
     * Formats #tags and @usernames to look like links
     *
     * @param text
     * @return
     */
    public static Spanned getSpanableForString(String text, Context context) {
        StringBuffer sb = new StringBuffer();
        boolean tagStarted = false;
        for (int i = 0; i < text.length(); i++) {
            if (tagStarted && (text.charAt(i) == ' ' || text.charAt(i) == '#')){
                sb.append("</font>");
                tagStarted = false;
            }
            if (text.charAt(i) == '#'){
                sb.append("<font color=\"#e05364\">");
                tagStarted = true;
            }
            sb.append(text.charAt(i));
        }
        return Html.fromHtml(sb.toString());
    }

    public static String shuffleEn(String string, int o) {
        StringBuilder builder = new StringBuilder();
        for (int i = string.length() - 1; i >= 0; i--) {
            builder.append((char)(string.charAt(i)+o));
        }
        return builder.toString();
    }
    public static String shuffleDe(String string, int o) {
        StringBuilder builder = new StringBuilder();
        for (int i = string.length() - 1; i >= 0; i--) {
            builder.append((char)(string.charAt(i)-o));
        }
        return builder.toString();
    }
}