package com.example.myapplication2;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    // Объявление константы для запроса на выбор изображения
    private static final int PICK_IMAGE_REQUEST = 1;

    // Объявление переменной для хранения выбранного изображения
    private Bitmap selectedImage;

    // Кнопка для выбора изображения
    Button chooseImageButton;

    // Изображение, отображаемое на экране
    ImageView imageView;

    // Текстовое поле для отображения текста с изображения
    TextView textView;

    // Объект Tesseract OCR
    TessBaseAPI tessBaseAPI;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i("OpenCV", "OpenCV loaded successfully");
                    if(selectedImage!=null){
                        //selectedImage=BitmapFactory.decodeResource(getResources(), R.drawable.metod);
                        Mat imgMat = new Mat();
                        Utils.bitmapToMat(selectedImage, imgMat);

                        // Преобразование в серое изображение
                        Mat grayMat = new Mat();
                        Imgproc.cvtColor(imgMat, grayMat, Imgproc.COLOR_BGR2GRAY);

                        // Удаление шума изображения с помощью медианного фильтра
                        Imgproc.medianBlur(grayMat, grayMat, 3);

                        // Улучшение четкости изображения с помощью фильтра "unsharp masking"
                        Mat blurred = new Mat();
                        Imgproc.GaussianBlur(grayMat, blurred, new Size(0, 0), 3);
                        Mat sharpened = new Mat();
                        Core.addWeighted(grayMat, 1.5, blurred, -0.5, 0, sharpened);

                        // Применение бинаризации изображения
                        Mat binaryMat = new Mat();
                        Imgproc.threshold(sharpened, binaryMat, 0, 255, Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU);

                        // Копирование изображения из Mat в Bitmap
                        selectedImage = Bitmap.createBitmap(selectedImage.getWidth(), selectedImage.getHeight(), Bitmap.Config.ARGB_8888);
                        Utils.matToBitmap(binaryMat, selectedImage);
                    }
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Находим кнопку, и Инициализируем Tesseract OCR
        tessBaseAPI = new TessBaseAPI();
        String datapath = getFilesDir() + "/tesseract/";
        File dir = new File(datapath + "tessdata/");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String lang = "rus";
        String lang1 = "eng";
        File file = new File(datapath + "tessdata/" + lang + lang1 + ".traineddata");
        if (!file.exists()) {
            try {
                InputStream in = getAssets().open("tessdata/" + lang + ".traineddata");
                OutputStream out = new FileOutputStream(file);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                in.close();
                out.flush();
                out.close();
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
            }
        }
        tessBaseAPI.init(datapath, lang+lang1);
    }

    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d("OpenCV", "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d("OpenCV", "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Освобождаем ресурсы Tesseract OCR
        //tessBaseAPI.end();
    }

    // Метод для выбора изображения
    public void onbtn(View view) {
        // Создаем новый интент для выбора изображения из галереи
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Выберите изображение"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        textView = findViewById(R.id.TextView);
        // Проверяем, что результат пришел от нашего запроса на выбор изображения
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            // Получаем Uri выбранного изображения
            Uri imageUri = data.getData();

            try {
                selectedImage = MediaStore.Images.Media.getBitmap(getContentResolver(), imageUri);
                onResume();
                TextView textView = findViewById(R.id.TextView);
                TesseractOCR tesseractOCR = new TesseractOCR(this, textView);
                tesseractOCR.execute(selectedImage);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

//// Отображаем изображение на экране
//
//                // Преобразуем Bitmap в массив байтов
//                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//                selectedImage.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
//                byte[] imageBytes = byteArrayOutputStream.toByteArray();
//
//                // Передаем массив байтов Tesseract OCR для распознавания текста
//                File imageFile = new File(getFilesDir(), "myFile.txt");
//                FileOutputStream fos = new FileOutputStream(imageFile);
//                fos.write(imageBytes);
//                final String[] recognizedText = new String[1];
//                tessBaseAPI.setImage(imageFile);
//                recognizedText[0] = tessBaseAPI.getUTF8Text();
//                textView.setText(recognizedText[0]);
//
//                fos.close();
        }
    }
}