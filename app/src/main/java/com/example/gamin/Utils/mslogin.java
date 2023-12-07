package com.example.gamin.Utils;

import android.webkit.WebView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;


public final class mslogin {

    public static boolean isFinished = false;
    public static String accessToken;


    final static String loginUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?"
            + "client_id=36c6ad8b-d9c9-43ed-aca0-56d694849ac1&"
            + "response_type=code&"
            + "scope=XboxLive.signin%20offline_access&"
            + "redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf";
    //final static String redirectUrlSuffix = "https://login.live.com/oauth20_desktop.srf?code=";
    final static String authTokenUrl = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";

    final static String xblAuthUrl = "https://user.auth.xboxlive.com/user/authenticate";

    final static String xstsAuthUrl = "https://xsts.auth.xboxlive.com/xsts/authorize";

    //final String mcStoreUrl = "https://api.minecraftservices.com/entitlements/mcstore";

    final static String mcLoginUrl = "https://api.minecraftservices.com/authentication/login_with_xbox";

    //final String mcProfileUrl = "https://api.minecraftservices.com/minecraft/profile";


    public static void start(WebView webview) {
        webview.loadUrl(loginUrl);


    }

    public static void acquireAccessToken(String code, File file) {
        try {
            URL url = new URL(authTokenUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            String urlParameters = "client_id=36c6ad8b-d9c9-43ed-aca0-56d694849ac1"
                    + "&scope=XboxLive.signin%20offline_access"
                    + "&code="+code
                    + "&redirect_uri=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf"
                    + "&grant_type=authorization_code";
            byte[] input = urlParameters.getBytes(StandardCharsets.UTF_8);
            System.out.println(urlParameters);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            con.setRequestProperty("charset", "utf-8");
            con.setRequestProperty("Content-Length", Integer.toString(input.length));
            try(DataOutputStream os = new DataOutputStream(con.getOutputStream())) {
                os.write(input);
            }

            InputStream stream;
            stream = con.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8), 8);
            String result = reader.readLine();
            System.out.println(result);
            String msAccessToken = result.substring(result.indexOf("access_token")+15, result.length()-2);
            System.out.println(msAccessToken);
            xboxLogin(msAccessToken, file);




        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public static void xboxLogin(String code, File file) {
        try {
            URL url = new URL(xblAuthUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            String urlParameters = "{\r\n"
                    + "    \"Properties\": {\r\n"
                    + "        \"AuthMethod\": \"RPS\",\r\n"
                    + "        \"SiteName\": \"user.auth.xboxlive.com\",\r\n"
                    + "        \"RpsTicket\": \"d="+code+"\"\r\n"
                    + "    },\r\n"
                    + "    \"RelyingParty\": \"http://auth.xboxlive.com\",\r\n"
                    + "    \"TokenType\": \"JWT\"\r\n"
                    + " }";
            byte[] input = urlParameters.getBytes(StandardCharsets.UTF_8);
            System.out.println(urlParameters);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("charset", "utf-8");
            con.setRequestProperty("Content-Length", Integer.toString(input.length));
            try(DataOutputStream os = new DataOutputStream(con.getOutputStream())) {
                os.write(input);
            }

            InputStream stream;
            stream = con.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8), 8);
            String result = reader.readLine();
            System.out.println(result);
            String xblToken = result.substring(result.indexOf("Token")+8, result.indexOf("DisplayClaims")-3);
            System.out.println(xblToken);
            xsts(xblToken, file);




        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void xsts(String code, File file) {

        try {
            URL url = new URL(xstsAuthUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            String urlParameters = " {\r\n"
                    + "    \"Properties\": {\r\n"
                    + "        \"SandboxId\": \"RETAIL\",\r\n"
                    + "        \"UserTokens\": [\""+code+"\"]\r\n"
                    + "    },\r\n"
                    + "    \"RelyingParty\": \"rp://api.minecraftservices.com/\",\r\n"
                    + "    \"TokenType\": \"JWT\"\r\n"
                    + " }";
            byte[] input = urlParameters.getBytes(StandardCharsets.UTF_8);
            System.out.println(urlParameters);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("charset", "utf-8");
            con.setRequestProperty("Content-Length", Integer.toString(input.length));
            try(DataOutputStream os = new DataOutputStream(con.getOutputStream())) {
                os.write(input);
            }

            InputStream stream;
            stream = con.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8), 8);
            String result = reader.readLine();
            System.out.println(result);
            String xtstToken = result.substring(result.indexOf("Token")+8, result.indexOf("DisplayClaims")-3);
            String uhs = result.substring(result.indexOf("uhs")+6, result.length()-5);
            System.out.println(xtstToken);
            System.out.println(uhs);
            getAccessToken(xtstToken,uhs,file);





        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void getAccessToken(String token, String uhs, File file) {
        try {
            URL url = new URL(mcLoginUrl);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            String urlParameters = "{\r\n"
                    + "    \"identityToken\": \"XBL3.0 x="+uhs+";"+token+"\"\r\n"
                    + " }";
            byte[] input = urlParameters.getBytes(StandardCharsets.UTF_8);
            System.out.println(urlParameters);
            con.setDoOutput(true);
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("charset", "utf-8");
            con.setRequestProperty("Content-Length", Integer.toString(input.length));
            try(DataOutputStream os = new DataOutputStream(con.getOutputStream())) {
                os.write(input);
            }

            InputStream stream;
            stream = con.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8), 8);
            System.out.println(reader.readLine());
            System.out.println(reader.readLine());
            System.out.println(reader.readLine());
            System.out.println(reader.readLine());
            String result = reader.readLine();
            System.out.println(result);
            accessToken = result.substring(result.indexOf("access_token")+17, result.length()-2);
            isFinished = true;
            System.out.println(accessToken);
            FileWriter filewriter = new FileWriter(file);
            filewriter.write(LocalDate.now().getDayOfYear() + "\n");
            filewriter.write(accessToken);
            filewriter.close();



        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void join(String serverHash) throws NumberFormatException, IOException {
        FileReader filereader = new FileReader("/storage/emulated/0/Yeni Metin Belgesi.txt");
        BufferedReader freader = new BufferedReader(filereader);
        if (Integer.parseInt(freader.readLine()) == LocalDate.now().getDayOfYear()) {
            try {
                accessToken = freader.readLine();
                System.out.println(accessToken);
                URL url = new URL("https://sessionserver.mojang.com/session/minecraft/join");
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                String urlParameters = " {\r\n"
                        + "    \"accessToken\": \""+accessToken+"\",\r\n"
                        + "    \"selectedProfile\": \"fd7769013b144a45b2fb689080b68c7b\",\r\n"
                        + "    \"serverId\": \""+serverHash+"\"\r\n"
                        + "  }";
                byte[] input = urlParameters.getBytes(StandardCharsets.UTF_8);
                System.out.println(urlParameters);
                con.setDoOutput(true);
                con.setRequestMethod("POST");
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Accept", "application/json");
                con.setRequestProperty("charset", "utf-8");
                con.setRequestProperty("Content-Length", Integer.toString(input.length));
                try(DataOutputStream os = new DataOutputStream(con.getOutputStream())) {
                    os.write(input);
                }

                System.out.println("asasadadsdasds"+con.getResponseCode());
                InputStream stream;
                stream = con.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8), 8);
                String result = reader.readLine();
                System.out.println(result);




            } catch (IOException e) {
                e.printStackTrace();
            }

        }else {
            freader.close();
            System.out.println("launch bşttş");
        }





    }

}







