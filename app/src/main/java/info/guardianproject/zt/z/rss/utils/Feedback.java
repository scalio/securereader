package info.guardianproject.zt.z.rss.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.widget.Toast;
import info.guardianproject.zt.R;
import info.guardianproject.zt.z.rss.constants.Constants;

import java.util.Iterator;

public class Feedback {


  public static String getPackageShortName(final String packageName) {
        return packageName.substring("info.guardianproject.zt.z.rss".length() + 1);
   }

  public static void addSystemInfoTo(StringBuilder report) {
    report.append("-------------------------------\n\n");
    report.append("System info:\n");
    report.append("Model:" + Build.MODEL + "\n");
    report.append("Display:" + Build.DISPLAY + "\n");
    report.append("Release:" + Build.VERSION.RELEASE + "\n");
    report.append("Sdk:" + Build.VERSION.SDK_INT + "\n");
    int memory = (int) (Runtime.getRuntime().totalMemory() / 1024);
    report.append("Total memory:" + memory + "K\n");
    report.append("Storage state:" + Environment.getExternalStorageState() + "\n");
  }

  public static void addIntentInfoTo(StringBuilder report, Intent intent) {
    if (intent == null) {
      // Nothing to do.
      return;
    }
    report.append("\n");
    report.append("Intent:\n");
    report.append("Package:").append(intent.getComponent().getPackageName()).append("\n");
    report.append("Class:").append(intent.getComponent().getClassName()).append("\n");
    report.append("\n");
    if (intent.getExtras() != null) {
      Iterator<String> iterator = intent.getExtras().keySet().iterator();
      while (iterator.hasNext()) {
        String key = iterator.next();
        report.append(key).append(":");
        // Skip some extras.
        if ("html".equals(key) ||
            key.contains("payment")) {
          report.append("X\n");
          continue;
        }
        report.append(String.valueOf(intent.getExtras().get(key)))
            .append("\n");
      }
    }

  }

//    public static void addAppInfoTo(StringBuilder report, Context context, final String packageName) {
//        report.append("\n");
//        report.append("App version:").append(Feedback.getPackageNameAndVersion(context, packageName)).append("\n");
//        report.append("\n");
//    }
    public static String  getPackageVersion(Context context) {
        try {
            // This should be the same as the package from AndroidManifest.xml
            String applicationPackage = R.class.getPackage().getName();
            return context.getPackageManager().getPackageInfo(applicationPackage, 0).versionName;
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
  public static void sendEmail(Context context, String title, String subject, String body) {
    Intent sendIntent = new Intent(Intent.ACTION_SEND);
    sendIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{Constants.CRASH_REPORTS_EMAIL});
    sendIntent.putExtra(Intent.EXTRA_TEXT, body);
    sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
    sendIntent.setType("message/rfc822");

    Intent chooseIntent = Intent.createChooser(sendIntent,title);
    context.startActivity(chooseIntent);
  }
    public static void sendEmailWithAttach(Context context, String title, String subject, String body, Uri uri) {
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        sendIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        sendIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{Constants.CRASH_REPORTS_EMAIL});
        sendIntent.putExtra(Intent.EXTRA_TEXT, body);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        sendIntent.setType("message/rfc822");
        sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
        Intent chooseIntent = Intent.createChooser(sendIntent, title);
        context.startActivity(chooseIntent);
    }


    public static void sendEmail2(Context context, String subject, String body) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        String uriText = "mailto:"+
                "?subject=" + Uri.encode(subject) +
                "&body=" + Uri.encode(body);
        Uri uri = Uri.parse(uriText);

        intent.setData(uri);
        try {
            context.startActivity(Intent.createChooser(intent, "Send Email"));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context,
                    context.getString(R.string.noEmailClients),
                    Toast.LENGTH_SHORT).show();
        }
    }


  public static void giveFeedback(Activity activity, String packageName) {
    giveFeedback(activity, null, packageName);
  }

    public static String getPackageNameAndVersion(Context context, final String packageName) {
        String packageNamePart = getPackageShortName(packageName);
        String packageVersion = getPackageVersion(context);
        return packageNamePart + " (" + packageVersion + ")";
    }


  public static void giveFeedback(Activity activity, String extra, String packageName) {
    String subject = "Feedback - " + getPackageNameAndVersion(activity,packageName);
    StringBuilder body = new StringBuilder("Hello,\n\n\n\n");
    addSystemInfoTo(body);
//    addAppInfoTo(body, activity,packageName);
    addIntentInfoTo(body, activity.getIntent());
    if (extra != null) {
      body.append("\n\n").append(extra);
    }
    sendEmail(activity,  activity.getString(R.string.sendFeedback), subject, body.toString());
  }
}
