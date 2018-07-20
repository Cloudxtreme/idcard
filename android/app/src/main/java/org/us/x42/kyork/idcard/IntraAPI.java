package org.us.x42.kyork.idcard;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;

public class IntraAPI {
    private static final String UID = "c7c56a88eef81dbcd50dea5d1384d1e9448e786699672d78db78e29c9f2584e7";
    private static final String SECRET = "bea0ca93b5bc8606cddc318c083f89008e09c20bb7568b4bc8925c3fe9419e6b";

    private JSONObject token = null;
    private HashMap<String,JSONObject> users = new HashMap<String,JSONObject>();

    public IntraAPI() { }

    private static String getResponse(URLConnection conn) throws IOException, JSONException {
        InputStream stream = conn.getInputStream();
        String response = "";
        byte[] buffer = new byte[4096];
        int b;
        while ((b = stream.read(buffer)) != -1)
            response += new String(buffer, 0, b);
        stream.close();
        return response;
    }

    private boolean checkToken() {
        if (this.token != null) {
            try {
                long created_at = this.token.getLong("created_at");
                long expires_in = this.token.getLong("expires_in");

                if ((System.currentTimeMillis() / 1000) < (created_at + expires_in))
                    return true;
            }
            catch (JSONException e) {
                e.printStackTrace(System.err);
            }
        }

        return false;
    }

    private void generateToken() throws IOException, JSONException {
        URL url = new URL("https://api.intra.42.fr/oauth/token");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");

        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        writer.write("grant_type=client_credentials&client_id=" + UID + "&client_secret=" + SECRET);
        writer.flush();
        writer.close();

        JSONObject response = new JSONObject(getResponse(conn));
        if (!response.has("access_token"))
            throw new JSONException("Failed to retrieve token!");

        this.token = response;
    }

    private String apiCall(String apipath) throws IOException, JSONException {
        if (!this.checkToken())
            this.generateToken();
        URL url = new URL("https://api.intra.42.fr" + apipath);
        URLConnection conn = url.openConnection();
        conn.setRequestProperty("Authorization", "Bearer " + this.token.get("access_token"));
        return getResponse(conn);
    }

    public boolean isCached(String login) {
        return (this.users.containsKey(login));
    }

    public JSONObject queryUser(String login, boolean useCached) throws IOException, JSONException {
        if (useCached) {
            JSONObject user = this.users.get(login);
            if (user != null)
                return user;
        }

        JSONObject user = new JSONObject(this.apiCall("/v2/users/" + login));
        this.users.put(login, user);
        return user;
    }

    /* - [JSON Getters for User Data] ------------------------------------------- */

    public String getFullName(String login) {
        try {
            JSONObject user = this.users.get(login);
            if (user != null)
                return user.getString("first_name") + " " + user.getString("last_name");
        }
        catch (JSONException e) {
            e.printStackTrace(System.err);
        }
        return null;
    }

    public String getTitle(String login) {
        String title = login;

        try {
            JSONObject user = this.users.get(login);
            if (user != null) {
                JSONArray titles_users = user.getJSONArray("titles_users");
                int title_id = -1;
                for (int i = 0; i < titles_users.length(); i++) {
                    JSONObject titles_user = titles_users.getJSONObject(i);
                    if (titles_user.getBoolean("selected")) {
                        title_id = titles_user.getInt("title_id");
                        break;
                    }
                }

                JSONArray titles = user.getJSONArray("titles");
                if (title_id != -1) {
                    for (int i = 0; i < titles.length(); i++) {
                        JSONObject title_obj = titles.getJSONObject(i);
                        if (title_id == title_obj.getInt("id")) {
                            title = title_obj.getString("name").replaceAll("%login", login);
                            break;
                        }
                    }
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace(System.err);
        }

        return title;
    }

    public String getImageURL(String login) {
        try {
            JSONObject user = this.users.get(login);
            if (user != null)
                return user.getString("image_url");
        }
        catch (JSONException e) {
            e.printStackTrace(System.err);
        }
        return (null);
    }

    public JSONArray getCursusArray(String login) {
        try {
            JSONObject user = this.users.get(login);
            if (user != null)
                return user.getJSONArray("cursus_users");
        }
        catch (JSONException e) {
            e.printStackTrace(System.err);
        }

        return null;
    }

    public JSONObject getCursus(String login, String cursus_name) {
        try {
            JSONObject user = this.users.get(login);
            if (user != null) {
                JSONArray cursus_users = user.getJSONArray("cursus_users");
                for (int i = 0; i < cursus_users.length(); i++) {
                    JSONObject cursus_user = cursus_users.getJSONObject(i);
                    JSONObject cursus = cursus_user.getJSONObject("cursus");
                    if (cursus_name.equals(cursus.getString("name")))
                        return cursus_user;
                }
            }
        }
        catch (JSONException e) {
            e.printStackTrace(System.err);
        }

        return null;
    }

    public String getPhone(String login) {
        try {
            JSONObject user = this.users.get(login);
            if (user != null)
                return user.getString("phone");
        }
        catch (JSONException e) {
            e.printStackTrace(System.err);
        }
        return (null);
    }
}
