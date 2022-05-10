package com.ishaia.activities;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.ishaia.BuildConfig;
import com.ishaia.R;
import com.ishaia.global.App;
import com.ishaia.utils.Const;
import com.ishaia.utils.FileUtils;
import com.theartofdev.edmodo.cropper.CropImage;
import com.xw.repo.BubbleSeekBar;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static android.widget.ListPopupWindow.MATCH_PARENT;
import static android.widget.ListPopupWindow.WRAP_CONTENT;
import static com.ishaia.utils.Const.KEY_IMAGE_URL;
import static com.ishaia.utils.Const.KEY_MUSIC_PATH;




/**
 * That class have 3 main usages
 * 1. handle from where the user choose the picture
 * 2. send the picture with parameters of the scale to the server
 * 3. save the song receive from the server
 * <p>
 * In details
 * 1. The user can choose the picture between 2 options
 * capture from camera, internal storage device
 * <p>
 * 2. The user will config the scale and speed of the song
 * that will be send to the server
 * <p>
 * 3. receive from the server picture that contain all the
 * server process and MIDI file of that song
 */
public class ChooseImageActivity extends AppCompatActivity {

    /* global declaration */
    private PhotoView img;
    private ImageButton buttonChooseCamera;
    private ImageButton buttonChooseGallery;
    private ImageButton buttonUpload;

    /* constant */
    /* request codes for recognize each permission and request that will be needed  */
    private static final int IMG_REQUEST = 777;
    public static final int REQUEST_IMAGE = 100;
    public static final int REQUEST_PERMISSION = 200;
    private static final int REQUEST_CROP_GALLERY = 409;
    private static final int REQUEST_CROP_CAMERA = 491;
    private static int REQUEST_CODE = 0;
    private static final int minSongSpeed = 1;
    private static final int maxSongSpeed = 300;

    /* use TAG for know in what activity we are */
    String imageFilePath = "";
    private final String TAG = getClass().getSimpleName();
    private ProgressDialog progressDialog = null;
    private long downloadID = -1;

    /* anther global declaration*/
    private BottomSheetDialog bottomSheetDialog;
    private BubbleSeekBar song_speed, amount_scale;
    private RadioGroup scale_type;
    private RadioButton scale_selected;
    private ImageButton OK;
    private Button minus, plus;
    public TextView scale, speeddisplay;
    private int selectedScale, songSpeed, scaleAmount;
    private Uri selectedImgUri;

    //  When download completed this registered broadcast will be called
    BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            //  checking that is our downloaded file, cuz it broadcast
            if (downloadID == intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID,
                    -1)) {
                startActivity();
            }
        }
    };

    private void startActivity() {
        hideProgressDialog();
        //  start playing audio and showing notes activity with image url & local midi path
        startActivity(new Intent(this, ImageScrollActivity.class)
                .putExtra("imgPath", Const.imageUrl)
                .putExtra("musicPath", Const.musicPath));
    }

    /**
     * @param savedInstanceState - position of Instance
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_choose_image);
        //check if user give permission to internal storage and etc
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                PackageManager.PERMISSION_GRANTED) {
            //ask the user to give permission to internal storage and etc
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION);
        }

        img = findViewById(R.id.image_view);

        /* define the swip up/down button and show the dialog when pressed*/
        findViewById(R.id.llBottomDialog).setOnTouchListener((v, event) -> {
            showBottomDialog();
            return false;
        });
        showBottomDialog();
        /* hid bar that we decide to not use */
        getSupportActionBar().hide();
    }

    /**
     * here handle the respond of the user for gave permission to the storage
     *
     * @param requestCode  - stand for check the type of request
     * @param permissions  - stand for check if the user gave permission
     * @param grantResults - must be big then 0 for continue
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSION && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Thanks for granting Permission", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Cannot continue without permission", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(ChooseImageActivity.this, MainActivity.class);
                startActivity(intent);
            }
        }

    }

    /**
     * here we config the buttons dialog
     * the buttons of choose image from camera of gallery
     * and upload image button
     */
    private void showBottomDialog() {
        // Bottom dialog - create and load component
        bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.layout_bottom_dialog);
        buttonChooseCamera = bottomSheetDialog.findViewById(R.id.button_choose_image_camera);
        buttonChooseGallery = bottomSheetDialog.findViewById(R.id.button_choose_image_gallery);
        buttonUpload = bottomSheetDialog.findViewById(R.id.button_upload);

        /**
         *  make button clickable/not clickable
         *  we dont want the user has option to click upload before he choose picture
         */
        buttonUpload.setEnabled(!imageFilePath.equals(""));
        buttonChooseCamera.setEnabled(imageFilePath.equals(""));
        buttonChooseGallery.setEnabled(imageFilePath.equals(""));

        /* define action if the user tuch the buttons*/
        buttonChooseCamera.setOnClickListener(v -> openCameraIntent());
        buttonChooseGallery.setOnClickListener(v -> selectImageFromGallery());

        buttonUpload.setOnClickListener(v -> {
            /* hide the button bottom sheet dialog if upload button pressed*/
            bottomSheetDialog.dismiss();

            /* open the window of config scale and song speed*/
            openDialog();
        });
        /* rest the last user config*/
        bottomSheetDialog.findViewById(R.id.button_reset).setOnClickListener(v -> showCaptureAgainDialog());
        bottomSheetDialog.show();
    }

    /**
     * @param url - path to download midi file and picture
     */
    public void downloadFile(String url) {
        try {
            // TODO: 06-06-2020 Change file name from here
            //delete last file if exist
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "ResponseMidi.mid"); // downloading file name.
            file.delete();
            Const.musicPath = file.getAbsolutePath();
            DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            Uri Download_Uri = Uri.parse(url);
            DownloadManager.Request request = new DownloadManager.Request(Download_Uri);
            request.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
            request.setAllowedOverRoaming(false);
            request.setTitle(file.getName());
            request.setDescription("Downloading...");
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, file.getName());
            assert downloadManager != null;

            // Assign enqueue id to 'downloadID' to check whether download call is from here or not.
            downloadID = downloadManager.enqueue(request);

            // Register broadcast receiver when download is completed 'onDownloadComplete' will be called.
            registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

            // Saving the response image url & local music path
            getSharedPreferences(BuildConfig.APPLICATION_ID, MODE_PRIVATE).edit()
                    .putString(KEY_IMAGE_URL, Const.imageUrl)
                    .putString(KEY_MUSIC_PATH, Const.musicPath).apply();

        } catch (Exception e) {
            Log.e(TAG, "downloadFile: ", e);
        }
    }

    /**
     * if the user choose to capture image from camera
     */
    private void openCameraIntent() {
        Intent pictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (pictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile;
            try {
                photoFile = createImageFile();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            /* after we capture the image we will store it */
            Uri photoUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", photoFile);
            pictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            startActivityForResult(pictureIntent, REQUEST_IMAGE);
        }
    }

    /**
     * will serve method that need image file, it define the name end extension
     *
     * @return File fot save image
     * @throws IOException - error exception
     */
    private File createImageFile() throws IOException {

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "IMG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        imageFilePath = image.getAbsolutePath();
        return image;
    }


    /**
     * if the user choose to capture image from gallery
     */
    private void selectImageFromGallery() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMG_REQUEST);
    }


    /**
     * here we crop the image that user choose from camera or gallery
     * we will check what code request we receive and take the right
     * action
     *
     * @param requestCode - stand for check the type of request from user
     * @param resultCode  -  stand for we can find the correct action to do
     * @param data        - data from user(picture
     */
    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Bitmap bitmap; /* crop image that choose from gallery */
        if (requestCode == IMG_REQUEST && resultCode == RESULT_OK && data != null) {
            REQUEST_CODE = REQUEST_CROP_GALLERY;
            Uri path = data.getData();
            selectedImgUri = path;
            CropImage.activity(path).start(this);
        } else if (requestCode == REQUEST_IMAGE) {
            if (resultCode == RESULT_OK) { /* crop image that capture from camera */
                REQUEST_CODE = REQUEST_CROP_CAMERA;
                selectedImgUri = Uri.fromFile(new File(imageFilePath));
                CropImage.activity(selectedImgUri).start(this);
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "You cancelled the operation", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == RESULT_OK && REQUEST_CODE == REQUEST_CROP_GALLERY) {
            REQUEST_CODE = 0;
            try {
                CropImage.ActivityResult result = CropImage.getActivityResult(data);
                Uri path = result.getUri();
                bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), path);
                img.setImageBitmap(addBorderToBitmap(bitmap));
                img.setVisibility(View.VISIBLE);

                imageFilePath = path.getPath();

                buttonChooseCamera.setEnabled(false);
                buttonChooseGallery.setEnabled(false);
                buttonUpload.setEnabled(true);

                bottomSheetDialog.dismiss();
                showCropConfirmationDialog(REQUEST_CROP_GALLERY, selectedImgUri);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else if (resultCode == RESULT_OK && REQUEST_CODE == REQUEST_CROP_CAMERA) {
            REQUEST_CODE = 0;
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            Uri path = result.getUri();
            img.setImageURI(path);
            bitmap = imageView2Bitmap(img);
            img.setImageBitmap(addBorderToBitmap(bitmap));
            img.setVisibility(View.VISIBLE);
            buttonChooseCamera.setEnabled(false);
            buttonChooseGallery.setEnabled(false);
            buttonUpload.setEnabled(true);

            bottomSheetDialog.dismiss();
            showCropConfirmationDialog(REQUEST_CROP_CAMERA, selectedImgUri);
        }
    }

    /**
     * after we crop the image we want the user to confirm the
     * image is ok, if not we will re-crop the picture again
     *
     * @param code - user answer about re-crop the image
     * @param uri  - image path
     */
    private void showCropConfirmationDialog(int code, Uri uri) {
        new Handler().postDelayed(() -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Confirmation")
                    .setMessage("Is this image ok?")
                    .setCancelable(false)
                    .setNegativeButton("No", (dialog, which) -> {
                        REQUEST_CODE = code;
                        CropImage.activity(uri).start(this);
                    }).setPositiveButton("Yes", null).show();
        }, 300);
    }

    /**
     * that stand for draw green rectangle around the image that been crop
     *
     * @param bmp - picture in bitmap
     * @return - picture with the green rectangle border
     */
    private Bitmap addBorderToBitmap(Bitmap bmp) {
        int width = bmp.getWidth();
        int height = bmp.getHeight();
        int borderWidth;
        if (height > width) {
            borderWidth = (int) (bmp.getHeight() * 0.01);
        } else {
            borderWidth = (int) (bmp.getWidth() * 0.01);
        }
        Bitmap bmpWithBorder = Bitmap.createBitmap(bmp.getWidth() + borderWidth * 2,
                bmp.getHeight() + borderWidth * 2, bmp.getConfig());
        Canvas canvas = new Canvas(bmpWithBorder);
        canvas.drawColor(Color.GREEN);
        canvas.drawBitmap(bmp, borderWidth, borderWidth, null);
        return bmpWithBorder;
    }


    /**
     * that stand for reset button
     * <p>
     * if the user want to reset the last operation we will
     * we will ensure that and restart last decision
     */
    private void showCaptureAgainDialog() {
        new Thread(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        new MaterialAlertDialogBuilder(this)
                .setTitle("Alert")
                .setMessage("Would you like to try again?")
                .setCancelable(false)
                .setPositiveButton("Yes", (dialog, which) -> {
                    resetOptions();
                }).setNegativeButton("No", null).show();
    }

    /**
     * if the user choose yes in the function above we will take that action
     */
    private void resetOptions() {
        imageFilePath = "";
        img.setVisibility(View.INVISIBLE);
        buttonUpload.setEnabled(false);
        buttonChooseCamera.setEnabled(true);
        buttonChooseGallery.setEnabled(true);

    }

    /* convert vew to bitmap drawable after crop image in one of th above cases*/
    private Bitmap imageView2Bitmap(ImageView view) {
        return ((BitmapDrawable) view.getDrawable()).getBitmap();
    }

    /**
     * after upload button click show progress dialog for
     * the status of the image processing
     * the flow - on progress - download song
     *
     * @param msg
     */
    private void showProgressDialog(String msg) {
        progressDialog = new ProgressDialog(this);
        if (msg.isEmpty())
            progressDialog.setMessage("on progress");
        else
            progressDialog.setMessage(msg);
        progressDialog.setCancelable(false);
        progressDialog.show();
    }

    /* after button press and similar cases we want to hide the progress dialog*/
    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * dialog for the user will define the following parameters
     * and upload the song
     * <p>
     * 1. speed of the song
     * 2. scale of the song b or #
     * 3. how many b or # the scale has
     */
    private void openDialog() {
        Dialog dialog = new Dialog(ChooseImageActivity.this);
        dialog.setContentView(R.layout.parameter_dialog_layout);

        /* define button of the dialog*/
        song_speed = dialog.findViewById(R.id.song_speed);
        amount_scale = dialog.findViewById(R.id.amout_scale);
        scale_type = dialog.findViewById(R.id.scale_type);
        OK = dialog.findViewById(R.id.dialog_ok);
        scale = dialog.findViewById(R.id.scale_text);
        speeddisplay = dialog.findViewById(R.id.speed_display);

        minus = dialog.findViewById(R.id.speed_decrease);
        plus = dialog.findViewById(R.id.speed_increase);
        speeddisplay.setText(String.valueOf(song_speed.getProgress()));

        /* set listeners to song speed buttons */
        minus.setOnClickListener(v -> {
            if (song_speed.getProgress() > minSongSpeed) {
                song_speed.setProgress(song_speed.getProgress() - 1);
            }

        });

        plus.setOnClickListener(v -> {
            if (song_speed.getProgress() < maxSongSpeed) {
                song_speed.setProgress(song_speed.getProgress() + 1);
            }

        });

        song_speed.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress,
                                          float progressFloat, boolean fromUser) {
                speeddisplay.setText(String.valueOf(song_speed.getProgress()));
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress,
                                              float progressFloat) {

            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress,
                                             float progressFloat, boolean fromUser) {

            }
        });

        /**
         *  set listener for choose amount of b or # that song contain
         */
        scale_type.setOnCheckedChangeListener((group, checkedId) -> {
            if (scale_type.getCheckedRadioButtonId() == R.id.scale_0) {
                amount_scale.setVisibility(View.GONE);
                scale.setVisibility(View.GONE);
            } else if (scale_type.getCheckedRadioButtonId() == R.id.scale_1) {
                amount_scale.setVisibility(View.VISIBLE);
                scale.setVisibility(View.VISIBLE);
                scale.setText("The value of ♭");
            } else {
                amount_scale.setVisibility(View.VISIBLE);
                scale.setVisibility(View.VISIBLE);
                scale.setText("The value of #");
            }
        });

        /* process the info and upload it to the server*/
        OK.setOnClickListener(v -> {
            if (scale_type.getCheckedRadioButtonId() == R.id.scale_0 ||
                    scale_type.getCheckedRadioButtonId() == R.id.scale_1 ||
                    scale_type.getCheckedRadioButtonId() == R.id.scale_2) {

                dialog.dismiss();
                selectedScale = scale_type.getCheckedRadioButtonId();
                songSpeed = song_speed.getProgress();
                scaleAmount = amount_scale.getProgress();
                uploadFile();
            } else {
                Toast.makeText(this, "Please Select Scale Type", Toast.LENGTH_SHORT).show();
            }

        });

        dialog.show();
        Objects.requireNonNull(dialog.getWindow()).setLayout(MATCH_PARENT, WRAP_CONTENT);

    }

    /* here we send the picture and info about scale and song speed to the server */
    private void uploadFile() {
        /* let the user know we start the progress*/
        Toast.makeText(ChooseImageActivity.this, "upload file", Toast.LENGTH_SHORT).show();
        showProgressDialog("");

        /* convert the picture to bitmap*/
        Bitmap bitmap = FileUtils.getBitmapFromImageView(img);
        File file = FileUtils.getImage(bitmap);

        // Defining the request type
        RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"),
                file);

        // Dividing the file into part for uploading
        MultipartBody.Part multipartBody = MultipartBody.Part.createFormData("file",
                file.getName(),
                requestFile);

        // defining the media type i.e. image, audio, video, etc,..
        RequestBody type = RequestBody.create(MediaType.parse("text/plain"), "image");

        // defining songSpeed into parameter
        RequestBody _songSpeed = RequestBody.create(MediaType.parse("text/plain"),
                String.valueOf(songSpeed));

        /** defining selected scale into parameter
         * 0 - regular scale
         * 1 - b scale
         * 2 - # scale
         */
        RequestBody _scaleType;
        if (selectedScale == R.id.scale_0) {
            _scaleType = RequestBody.create(MediaType.parse("text/plain"), "0");
        } else if (selectedScale == R.id.scale_1) {
            _scaleType = RequestBody.create(MediaType.parse("text/plain"), "1");
        } else {
            _scaleType = RequestBody.create(MediaType.parse("text/plain"), "2");
        }

        /*if the scale are not regular here we define amount of b or #  */
        RequestBody _amountScale;
        if (selectedScale == R.id.scale_0) {
            _amountScale = RequestBody.create(MediaType.parse("text/plain"), "null");
        } else {
            _amountScale = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(scaleAmount));
        }

        //  initializing Upload Api
        Call<ResponseBody> call = App.get().getApiService().uploadImgWithArgs(type, multipartBody,
                _songSpeed, _scaleType, _amountScale);

        /* let the user know that server start analyze the picture */
        Toast.makeText(ChooseImageActivity.this, "server process the image", Toast.LENGTH_LONG).show();

        // Here calling api
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                /* server send response */
                try {
                    hideProgressDialog();
                    if (response.isSuccessful()) {
                        /* create json for the server response*/
                        String strResponse = response.body().string();
                        JSONObject res = new JSONObject(strResponse);

                        /**
                         * Extract the picture and song root(url) path that
                         * return from the server
                         */
                        Const.imageUrl = res.optString("picture", "");
                        Const.musicPath = res.optString("song", "");

                        // if song url is not empty then start downloading it.
                        if (!Const.musicPath.isEmpty()) {
                            showProgressDialog("File is downloading,..");
                            downloadFile(Const.musicPath);
                        } else {
                            String error = res.optString("error", "");
                            Toast.makeText(ChooseImageActivity.this, error, Toast.LENGTH_SHORT).show();
                        }

                        Log.e(TAG, "onResponse: " + strResponse);
                    } else {
                        Log.e(TAG, "onResponse: " + response.message());
                        Toast.makeText(ChooseImageActivity.this, response.message(), Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "onResponseCatch: ", e);
                    Toast.makeText(ChooseImageActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }

            /* if for some reason on response failed show msg to the user */
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                hideProgressDialog();
                Log.e(TAG, "onFailure: " + t.getMessage());
                //Toast.makeText(ChooseImageActivity.this, t.getMessage(), Toast.LENGTH_SHORT).show();
                Toast.makeText(ChooseImageActivity.this, "you have issue with the internet", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
