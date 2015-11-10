package info.guardianproject.zt.z.rss.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.widget.Toast;
import info.guardianproject.securereaderinterface.R;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.KeyStore;


public class Network {

  public static boolean checkInternetConnectivity(Context context) {
    if (!hasInternetConnectivity(context)) {
      Toast.makeText(context, R.string.requires_internet_connection, Toast.LENGTH_LONG).show();
      return false;
    }
    return true;
  }

  public static boolean hasInternetConnectivity(Context context) {
    ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connManager.getActiveNetworkInfo();
    return networkInfo != null && networkInfo.isConnected();
  }

  public static boolean hasWifiConnectivity(Context context) {
    ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo networkInfo = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
    return networkInfo != null && networkInfo.isConnected();
  }

    private static DefaultHttpClient getHttpClient() {
        DefaultHttpClient httpClient = null;
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);
            HttpProtocolParams.setUserAgent(params, "android agent");

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            httpClient = new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            httpClient = new DefaultHttpClient();
        }

//        Credentials credentials = new Credentials();
//        httpClient.getCredentialsProvider().setCredentials(
//                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
//                new UsernamePasswordCredentials(credentials.getUserName(), credentials.getUserPass()));
        return httpClient;

    }

    public static String getDataViaHttp(String url) throws IOException {
        HttpClient httpClient = getHttpClient();
        HttpResponse response;
        String responseString = null;
        HttpGet httpRequest = new HttpGet(url);
        response = httpClient.execute(httpRequest);
        StatusLine statusLine = response.getStatusLine();
        if(statusLine.getStatusCode() == HttpStatus.SC_OK){
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            out.close();
            responseString = out.toString();
        } else{
            //Closes the connection.
            response.getEntity().getContent().close();
            throw new IOException(statusLine.getReasonPhrase());
        }
        return responseString;
    }


}
