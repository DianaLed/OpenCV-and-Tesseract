package com.example.myapplication2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class TesseractOCR extends AsyncTask<Bitmap, Void, String> {

    private Context context;
    private TextView textView;

    public TesseractOCR(Context context, TextView textView) {
        this.context = context;
        this.textView = textView;
    }

    @Override
    protected String doInBackground(Bitmap... bitmaps) {
        String result = null;
        try {
            TessBaseAPI tessBaseAPI = new TessBaseAPI();
            String datapath = context.getFilesDir() + "/tesseract/";
            File dir = new File(datapath + "tessdata/");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String lang = "rus";
            String lang1 = "eng";
            File file = new File(datapath + "tessdata/" + lang + lang1 + ".traineddata");
            if (!file.exists()) {
                try {
                    InputStream in = context.getAssets().open("tessdata/" + lang + ".traineddata");
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
            tessBaseAPI.setImage(bitmaps[0]);
            result = tessBaseAPI.getUTF8Text();
            tessBaseAPI.end();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    protected void onPostExecute(String result) {
        if (result != null) {
            textView.setText(result);
        }
    }
}
