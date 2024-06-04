package com.example.gamin;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.StrictMode;
import android.text.method.ScrollingMovementMethod;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.gamin.Minecraft.Inventory;
import com.example.gamin.Minecraft.Slot;
import com.example.gamin.Render.Entity;
import com.example.gamin.Render.OpenGLUtils;
import com.example.gamin.Render.YourRenderer;
import com.example.gamin.Utils.BrokenHash;
import com.example.gamin.Utils.Collision;
import com.example.gamin.Utils.PacketUtils;
import com.example.gamin.Utils.VarInt;
import com.example.gamin.Utils.mslogin;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@SuppressWarnings({"BusyWait", "UseOfSystemOutOrSystemErr"})
public class MainActivity extends AppCompatActivity {

    private Socket socket;
    private String serverhash;
    private String accestoken;
    private DataInputStream is = null;
    private TextView chatMessages;
    private ImageButton[] imageButtons;
    private GLSurfaceView glSurfaceView;
    private YourRenderer renderer;

    private int otizmi = 0;


    private static List<Byte> bytetoByte(@NonNull byte[] bytes) {
        return IntStream.range(0, bytes.length).mapToObj(i -> bytes[i]).collect(Collectors.toList());
    }

    @NonNull
    private static byte[] readNBytes(InputStream is, int len) {
        byte[] out = new byte[len];
        for (int i = 0; i < len; i++) {
            try {
                out[i] = (byte) is.read();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return out;
    }

    @SuppressLint({"SetTextI18n", "DiscouragedApi", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().detectLeakedClosableObjects().penaltyLog().build());
        try {
            OpenGLUtils.loadTextures(getApplicationContext());
            Slot.loadAssetData(getApplicationContext());
            Collision.loadCollisionData(getApplicationContext());
            Entity.loadEntities(getApplicationContext());
        } catch (IOException | JSONException e) {
            throw new RuntimeException(e);
        }

        setContentView(R.layout.activity_main);

        TextView loginCheck = findViewById(R.id.tokenStatus);
        EditText ipField = findViewById(R.id.ipField);
        File ipFile = new File(getFilesDir() + "/ip.txt");
        @SuppressLint("UseSwitchCompatOrMaterialCode") Switch switch2 = findViewById(R.id.switch2);
        EditText nameField = findViewById(R.id.nameField);
        Button playButton = findViewById(R.id.playButton);
        Button loginButton = findViewById(R.id.loginButton);

        if (ipFile.exists()) {
            try {
                FileReader fileReader = new FileReader(ipFile);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                ipField.setText(bufferedReader.readLine());
                fileReader.close();
                bufferedReader.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            try {
                //noinspection ResultOfMethodCallIgnored
                ipFile.createNewFile();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        loginButton.setOnClickListener(view -> {
            setContentView(R.layout.login_sayfasi);
            WebView webview = findViewById(R.id.webView);
            mslogin.start(webview);
            EditText authcodeField = findViewById(R.id.editTextTextPersonName2);
            Button realLoginButton = findViewById(R.id.button3);
            realLoginButton.setOnClickListener(view1 -> {
                String authcode = String.valueOf(authcodeField.getText());
                Thread thread = new Thread(() -> {
                    System.out.println(authcode);
                    mslogin.acquireAccessToken(authcode, ipFile);
                });
                thread.start();
            });
        });
        try {
            File file = new File(getFilesDir() + "/Yeni Metin Belgesi.txt");
            if (file.exists()) {
                if (file.length() != 0) {
                    FileReader filereader = new FileReader(file);
                    BufferedReader freader = new BufferedReader(filereader);
                    if (Integer.parseInt(freader.readLine()) == LocalDate.now().getDayOfYear()) {
                        loginCheck.setText("token iyi");
                        loginCheck.setTextColor(getColor(R.color.green));
                        accestoken = freader.readLine();
                    } else {
                        loginCheck.setText("token yok");
                        loginCheck.setTextColor(getColor(R.color.red));
                    }
                    filereader.close();
                    freader.close();
                }
            } else {
                //noinspection ResultOfMethodCallIgnored
                file.createNewFile();
                loginCheck.setText("token yok");
                loginCheck.setTextColor(getColor(R.color.red));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        playButton.setOnClickListener(view -> {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

            try {
                FileWriter fileWriter = new FileWriter(ipFile);
                fileWriter.write(String.valueOf(ipField.getText()));
                fileWriter.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            setContentView(R.layout.gaming_sayfasi);
            /*imageButtons = new ImageButton[90];
            for (int i = 0; i < 90; i++) {
                imageButtons[i] = findViewById(getResources().getIdentifier("imageButton" + (i + 1), "id", getPackageName()));
            }*/
            /*View[] layouts = {
                    findViewById(R.id.chatBox), findViewById(R.id.inventory), findViewById(R.id.window)
            };*/

            chatMessages = findViewById(R.id.chatMessages);
            chatMessages.setMovementMethod(new ScrollingMovementMethod());

            //Button otizm = findViewById(R.id.button22);


            glSurfaceView = findViewById(R.id.glView);
            glSurfaceView.setEGLContextClientVersion(2);
            renderer = new YourRenderer(getApplicationContext());
            glSurfaceView.setRenderer(renderer);
            glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

            final float[] mPreviousX = {0};
            final float[] mPreviousY = {0};
            glSurfaceView.setOnTouchListener((view1, motionEvent) -> {
                int width = glSurfaceView.getWidth();
                int height = glSurfaceView.getHeight();
                float ratio = (float) height / width;
                view1.performClick();

                float x = motionEvent.getX();
                float y = motionEvent.getY();

                if (!(x > width * 1.7 / 2 && y > height * (ratio * 2 - 0.3) / (ratio * 2))) {
                    PacketUtils.isSneaking = false;
                }

                if (x > width / 2.0) {
                    PacketUtils.moveLeftRight = 0;
                    PacketUtils.moveForwardBack = 0;
                    PacketUtils.jump = false;
                    if ((x > width * 1.7 / 2 && y > height * (ratio * 2 - 0.3) / (ratio * 2))) {
                        PacketUtils.isSneaking = true;
                    }
                    if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                        float newYaw = PacketUtils.x_rot + x - mPreviousX[0];
                        float newPitch = PacketUtils.y_rot + y - mPreviousY[0];
                        if (newPitch > 90) newPitch = 90;
                        if (newPitch < -90) newPitch = -90;
                        PacketUtils.y_rot = newPitch;
                        if (newYaw > 180) newYaw = newYaw - 360;
                        if (newYaw < -180) newYaw = newYaw + 360;
                        PacketUtils.x_rot = newYaw;
                        PacketUtils.isRotating = true;
                    }

                    mPreviousX[0] = x;
                    mPreviousY[0] = y;
                } else if (x > 0 && x < width * 0.6 / 2 && y < height && y > height * (ratio * 2 - 0.6) / (ratio * 2)) {
                    //System.out.printf("x: %f  y: %f\n", x, y);
                    if (x < width * 0.2 / 2) PacketUtils.moveLeftRight = 1;//sol
                    if (x > width * 0.4 / 2) PacketUtils.moveLeftRight = -1;
                    if (y > height * (ratio * 2 - 0.2) / (ratio * 2))
                        PacketUtils.moveForwardBack = -1;//arka
                    if (y < height * (ratio * 2 - 0.4) / (ratio * 2))
                        PacketUtils.moveForwardBack = 1;
                    if (x > width * 0.2 / 2 && x < width * 0.4 / 2 && y > height * (ratio * 2 - 0.4) / (ratio * 2) && y < height * (ratio * 2 - 0.2) / (ratio * 2))
                        PacketUtils.jump = true;
                    else if (x > width * 0.2 / 2 && x < width * 0.4 / 2) {
                        PacketUtils.moveLeftRight = 0;
                        PacketUtils.jump = false;
                    } else if (y > height * (ratio * 2 - 0.4) / (ratio * 2) && y < height * (ratio * 2 - 0.2) / (ratio * 2)) {
                        PacketUtils.moveForwardBack = 0;
                        PacketUtils.jump = false;
                    }
                } else {
                    PacketUtils.moveLeftRight = 0;
                    PacketUtils.moveForwardBack = 0;
                    PacketUtils.jump = false;
                }

                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    PacketUtils.moveLeftRight = 0;
                    PacketUtils.moveForwardBack = 0;
                    PacketUtils.jump = false;
                    PacketUtils.isSneaking = false;
                }
                PacketUtils.getTargetedObject();

                return true;
            });


            /*otizm.setOnClickListener(view13 -> {
                otizmi++;
                if (otizmi % 2 == 1) {
                    //InventoryUtils.drawButton(glSurfaceView,renderer,"blaze_rod",findViewById(R.id.imageButton90))
                    //InventoryUtils.showInventory(layouts[1],(byte)0)
                    InventoryUtils.resizeInventory(layouts[0], 0);
                    InventoryUtils.resizeInventory(layouts[1], R.dimen.yuzelli);
                    InventoryUtils.resizeInventory(layouts[2], 0);
                } else {
                    //InventoryUtils.drawButton(glSurfaceView,renderer,"acacia_fence_gate",findViewById(R.id.imageButton90))
                    //InventoryUtils.hideInventory(layouts[1],(byte)0)
                    InventoryUtils.resizeInventory(layouts[0], R.dimen.yuzyetmis);
                    InventoryUtils.resizeInventory(layouts[1], 0);
                    InventoryUtils.resizeInventory(layouts[2], 0);
                }
            });*/

            Thread thread = new Thread(() -> eskimain(String.valueOf(ipField.getText()), String.valueOf(nameField.getText()), switch2.isChecked()));
            thread.start();

            Button sendMessage = findViewById(R.id.sendMessage);
            EditText messageField = findViewById(R.id.ChatField);
            sendMessage.setOnClickListener(view12 -> {
                System.out.println("mesaj gidiyır");
                List<Byte> mesaj = new ArrayList<>();
                System.out.println(messageField.getText().length());
                System.out.println(messageField.getText());
                mesaj.addAll(VarInt.writeVarInt(messageField.getText().length()));
                mesaj.addAll(bytetoByte(String.valueOf(messageField.getText()).getBytes()));
                PacketUtils.write((byte) 1, mesaj, switch2.isChecked());
            });

            Button respawnButton = findViewById(R.id.respawn);
            respawnButton.setOnClickListener(view14 -> {
                PacketUtils.motionY = 0;
                PacketUtils.write((byte) 0x16, Collections.singletonList((byte) 0), PacketUtils.isPremium);
            });

            @SuppressLint("UseSwitchCompatOrMaterialCode") Switch toggleSprint = findViewById(R.id.toggleSprint);
            toggleSprint.setOnCheckedChangeListener((compoundButton, b) -> PacketUtils.isSprinting = b);


        });
    }

    private void eskimain(String serverip, String name, boolean isPremium) {
        try {
            new Inventory((byte) 0, "playerInventory", 45);
            socket = new Socket(serverip, 25565);
            OutputStream os = socket.getOutputStream();
            PacketUtils.os = os;

            List<Byte> handshake = new ArrayList<>(Arrays.asList((byte) 47, (byte) 2));
            handshake.add(1, (byte) serverip.length());
            handshake.addAll(2, bytetoByte(serverip.getBytes()));
            byte[] port = ByteBuffer.allocate(2).putShort((short) 25565).array();
            handshake.addAll(2 + serverip.length(), bytetoByte(port));
            System.out.println("sa");
            PacketUtils.write((byte) 0, handshake, false);

            try {//FIXME I think I should wait for the server to respond before sending the login packet instead of sleeping
                Thread.sleep(10);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            List<Byte> login = new ArrayList<>(Collections.singletonList((byte) name.length()));
            login.addAll(bytetoByte(name.getBytes()));
            PacketUtils.write((byte) 0, login, false);

            InputStream input = socket.getInputStream();
            DataInputStream datainputstream = new DataInputStream(input);

            PacketUtils.isPremium = isPremium;
            if (isPremium) {
                System.out.println(VarInt.readVarInt(datainputstream));
                System.out.println(VarInt.readVarInt(datainputstream));
                int serveridlen = VarInt.readVarInt(datainputstream);
                System.out.println(serveridlen + "serveridlen");
                byte[] serveridbytes = readNBytes(datainputstream, serveridlen);
                int publickeylen = VarInt.readVarInt(datainputstream);
                System.out.println(publickeylen + "spublickeylen");
                byte[] publickeybytes = readNBytes(datainputstream, publickeylen);
                int verifytokenlen = VarInt.readVarInt(datainputstream);
                System.out.println(verifytokenlen + "verifytokenlen");
                byte[] verifytoken = readNBytes(datainputstream, verifytokenlen);

                byte[] sharedsecretbytes = new byte[16];
                Random random = new SecureRandom();
                random.nextBytes(sharedsecretbytes);
                SecretKey sharedsecret = new SecretKeySpec(sharedsecretbytes, "AES");

                Cipher pkcipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                X509EncodedKeySpec publicSpec = new X509EncodedKeySpec(publickeybytes);
                KeyFactory keyfactory = KeyFactory.getInstance("RSA");
                PublicKey publickey = keyfactory.generatePublic(publicSpec);
                pkcipher.init(Cipher.ENCRYPT_MODE, publickey);
                System.out.println(pkcipher.doFinal(sharedsecretbytes).length);
                System.out.println(pkcipher.doFinal(verifytoken).length);

                String serverid = new String(serveridbytes, StandardCharsets.UTF_8);
                System.out.println(serverid);

                serverhash = BrokenHash.sha1(serveridbytes, sharedsecretbytes, publickey.getEncoded());
                mslogin.join(serverhash);

                List<Byte> enResponse = new ArrayList<>();
                enResponse.addAll(VarInt.writeVarInt(pkcipher.doFinal(sharedsecretbytes).length));
                enResponse.addAll(bytetoByte(pkcipher.doFinal(sharedsecretbytes)));
                enResponse.addAll(VarInt.writeVarInt(pkcipher.doFinal(verifytoken).length));
                enResponse.addAll(bytetoByte(pkcipher.doFinal(verifytoken)));

                PacketUtils.write((byte) 1, enResponse, false);

                IvParameterSpec v = new IvParameterSpec(sharedsecretbytes);
                Cipher ecipher = Cipher.getInstance("AES/CFB8/NoPadding");
                ecipher.init(Cipher.ENCRYPT_MODE, sharedsecret, v);
                PacketUtils.ecos = new CipherOutputStream(os, ecipher);
                Cipher dcipher = Cipher.getInstance("AES/CFB8/NoPadding");
                dcipher.init(Cipher.DECRYPT_MODE, sharedsecret, v);
                InputStream n = new CipherInputStream(input, dcipher);
                is = new DataInputStream(n);
            } else {
                is = new DataInputStream(input);
            }
            Runnable listener = () -> {
                int compression = -1;
                int packetlen;
                int packetdlen;
                //noinspection InfiniteLoopStatement
                while (true) {
                    try {
                        packetlen = VarInt.readVarInt(is);
                        if (packetlen > 0) {
                            if (compression > -1) {
                                Inflater inflater = new Inflater();
                                packetdlen = VarInt.readVarInt(is);
                                byte[] packetData = new byte[packetdlen];
                                if (packetdlen == 0) {
                                    byte packetid = is.readByte();
                                    byte[] packetdata = readNBytes(is, packetlen - 2);
                                    runOnUiThread(() -> {
                                        try {
                                            PacketUtils.read(glSurfaceView, renderer, imageButtons, chatMessages, packetid, packetdata);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                } else {
                                    inflater.setInput(readNBytes(is, packetlen - VarInt.writeVarInt(packetdlen).size()));
                                    inflater.inflate(packetData);
                                    inflater.reset();

                                    ByteArrayInputStream packetDatastream = new ByteArrayInputStream(packetData);
                                    byte packetid = (byte) packetDatastream.read();
                                    byte[] packetdata = readNBytes(packetDatastream, packetDatastream.available());
                                    runOnUiThread(() -> {
                                        try {
                                            PacketUtils.read(glSurfaceView, renderer, imageButtons, chatMessages, packetid, packetdata);
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    });
                                }
                                inflater.end();

                            } else {
                                byte packetid = is.readByte();
                                byte[] packetdata = readNBytes(is, packetlen - 1);
                                if (packetid == packetlen && packetid == 3) {
                                    compression = VarInt.readVarInt(new ByteArrayInputStream(packetdata));
                                    PacketUtils.isCompressed = true;
                                }
                                runOnUiThread(() -> {
                                    try {
                                        PacketUtils.read(glSurfaceView, renderer, imageButtons, chatMessages, packetid, packetdata);
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                            }
                        }
                    } catch (IOException | DataFormatException e) {
                        e.printStackTrace();
                    }
                }
            };
            Executor execut = Executors.newSingleThreadExecutor();
            execut.execute(listener);

            runOnUiThread(() -> {
                TextView textViewX = findViewById(R.id.textViewX);
                TextView textViewY = findViewById(R.id.textViewY);
                TextView textViewZ = findViewById(R.id.textViewZ);

                @SuppressLint("SetTextI18n")
                Thread playerposthread = new Thread(() -> {
                    byte timeSincePos = 0;
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    List<Byte> clientSettings = new ArrayList<>();
                    //clientSettings.add((byte)5)
                    //clientSettings.addAll(bytetoByte("en_us".getBytes()))
                    clientSettings.add((byte) 2);
                    clientSettings.add((byte) 0);
                    clientSettings.add((byte) 1);
                    clientSettings.add((byte) 127);
                    PacketUtils.write((byte) 15, clientSettings, isPremium);

                    boolean onGround;
                    boolean onGround0;
                    List<Byte> playerPosAndLook = new ArrayList<>();
                    playerPosAndLook.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.x).array()));
                    playerPosAndLook.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.y).array()));
                    playerPosAndLook.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.z).array()));
                    playerPosAndLook.addAll(bytetoByte(ByteBuffer.allocate(4).putFloat(PacketUtils.x_rot).array()));
                    playerPosAndLook.addAll(bytetoByte(ByteBuffer.allocate(4).putFloat(PacketUtils.y_rot).array()));

                    try {
                        onGround0 = Collision.calculateMovement(PacketUtils.x, PacketUtils.y, PacketUtils.z, 1, 0.0)[3] == 1.0;
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    if (onGround0) {
                        playerPosAndLook.add((byte) 1);
                        PacketUtils.motionY = 0;
                    } else {
                        playerPosAndLook.add((byte) 0);
                    }
                    PacketUtils.write((byte) 6, playerPosAndLook, isPremium);
                    while (true) {

                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        try {
                            //Entity.moveEntities();
                            onGround = Collision.calculateMovement(PacketUtils.x, PacketUtils.y, PacketUtils.z, 1, 0)[3] == 1;
                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                        if (onGround) {
                            PacketUtils.motionY = 0;
                            if ((timeSincePos == 20 || PacketUtils.jump || PacketUtils.motionX != 0 || PacketUtils.motionZ != 0 || PacketUtils.moveLeftRight != 0 || PacketUtils.moveForwardBack != 0) && !PacketUtils.isRotating) {
                                List<Byte> playerpos = new ArrayList<>();

                                PacketUtils.calculateMovements();

                                playerpos.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.x).array()));
                                playerpos.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.y).array()));
                                playerpos.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.z).array()));
                                playerpos.add((byte) 1);
                                PacketUtils.write((byte) 4, playerpos, isPremium);
                                timeSincePos = 0;
                            } else if ((PacketUtils.moveForwardBack * 2 + PacketUtils.moveLeftRight) == 0 && PacketUtils.isRotating) {
                                List<Byte> playerlook = new ArrayList<>();
                                playerlook.addAll(bytetoByte(ByteBuffer.allocate(4).putFloat(PacketUtils.x_rot).array()));
                                playerlook.addAll(bytetoByte(ByteBuffer.allocate(4).putFloat(PacketUtils.y_rot).array()));
                                playerlook.add((byte) 1);
                                PacketUtils.write((byte) 5, playerlook, isPremium);
                                PacketUtils.isRotating = false;
                            } else if ((timeSincePos == 20 || (PacketUtils.moveForwardBack * 2 + PacketUtils.moveLeftRight) != 0) && PacketUtils.isRotating) {
                                List<Byte> playerposandlook = new ArrayList<>();

                                PacketUtils.calculateMovements();

                                playerposandlook.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.x).array()));
                                playerposandlook.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.y).array()));
                                playerposandlook.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.z).array()));
                                playerposandlook.addAll(bytetoByte(ByteBuffer.allocate(4).putFloat(PacketUtils.x_rot).array()));
                                playerposandlook.addAll(bytetoByte(ByteBuffer.allocate(4).putFloat(PacketUtils.y_rot).array()));
                                playerposandlook.add((byte) 1);
                                PacketUtils.write((byte) 6, playerposandlook, isPremium);
                                timeSincePos = 0;
                                PacketUtils.isRotating = false;
                            } else {
                                List<Byte> player = new ArrayList<>();
                                player.add((byte) 1);
                                PacketUtils.write((byte) 3, player, isPremium);
                                timeSincePos += 1;
                            }
                        } else if (!PacketUtils.isRotating) {
                            PacketUtils.calculateMovements();
                            List<Byte> playerpos = new ArrayList<>();
                            playerpos.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.x).array()));
                            playerpos.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.y).array()));
                            playerpos.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.z).array()));
                            playerpos.add((byte) 0);
                            PacketUtils.write((byte) 4, playerpos, isPremium);
                            timeSincePos = 0;
                        } else {
                            PacketUtils.calculateMovements();

                            List<Byte> playerposandlook = new ArrayList<>();
                            playerposandlook.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.x).array()));
                            playerposandlook.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.y).array()));
                            playerposandlook.addAll(bytetoByte(ByteBuffer.allocate(8).putDouble(PacketUtils.z).array()));
                            playerposandlook.addAll(bytetoByte(ByteBuffer.allocate(4).putFloat(PacketUtils.x_rot).array()));
                            playerposandlook.addAll(bytetoByte(ByteBuffer.allocate(4).putFloat(PacketUtils.y_rot).array()));
                            playerposandlook.add((byte) 0);
                            PacketUtils.write((byte) 6, playerposandlook, isPremium);
                            timeSincePos = 0;
                            PacketUtils.isRotating = false;

                        }
                        runOnUiThread(() -> {
                            textViewX.setText("X:" + PacketUtils.x + " Yaw:" + PacketUtils.x_rot);
                            textViewY.setText("Y:" + PacketUtils.y + " Pitch:" + PacketUtils.y_rot);
                            textViewZ.setText("Z:" + PacketUtils.z + " FPS:" + YourRenderer.fps);
                        });

                    }
                });

                playerposthread.start();
            });


        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException |
                 InvalidKeySpecException | InvalidKeyException | BadPaddingException |
                 IllegalBlockSizeException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();

        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {


        return super.onKeyDown(keyCode, event);
    }
}