package net.util;

import java.util.Hashtable;

import net.app.netdatatransfer.R;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class CreateQRImage {
    private static ImageView sweepIV;
    private Context context;
    private int QR_WIDTH = 200, QR_HEIGHT = 200;
    private int imageW, imageH;

    public CreateQRImage(String uri, ImageView img, Context context) {
        sweepIV = img;
        this.context = context;
        QR_WIDTH = dip2px(context, QR_WIDTH);
        QR_HEIGHT = dip2px(context, QR_HEIGHT);
        createQRImage(uri);
    }

    // 要转换的地址或字符串,可以是中文
    public void createQRImage(String url) {
        try {
            // 判断URL合法性
            if (url == null || "".equals(url) || url.length() < 1) {
                return;
            }
            Bitmap[] bitmaps = new Bitmap[2];

            BitmapFactory.Options bmpFactoryOptions = new BitmapFactory.Options();
            bmpFactoryOptions.inJustDecodeBounds = true;
            bitmaps[1] = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.qr_logo, bmpFactoryOptions);// logo图标
            bmpFactoryOptions.inSampleSize = 2;
            bmpFactoryOptions.inJustDecodeBounds = false;
            bitmaps[1] = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.qr_logo, bmpFactoryOptions);// logo图标
            imageW = bitmaps[1].getWidth();
            imageH = bitmaps[1].getHeight();
            int startW = QR_WIDTH / 2 - imageW / 2;
            int starH = QR_HEIGHT / 2 - imageH / 2;

            Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            // 图像数据转换，使用了矩阵转换
            BitMatrix bitMatrix = new QRCodeWriter().encode(url,
                    BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);
            int[] pixels = new int[QR_WIDTH * QR_HEIGHT];
            // 下面这里按照二维码的算法，逐个生成二维码的图片，
            // 两个for循环是图片横列扫描的结果
            for (int y = 0; y < QR_HEIGHT; y++) {
                for (int x = 0; x < QR_WIDTH; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * QR_WIDTH + x] = 0xff000000;
                    } else {
                        pixels[y * QR_WIDTH + x] = 0xffffffff;
                    }
                }
            }
            // 生成二维码图片的格式，使用ARGB_8888
            Bitmap bitmap = Bitmap.createBitmap(QR_WIDTH, QR_HEIGHT,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, QR_WIDTH, 0, 0, QR_WIDTH, QR_HEIGHT);
            bitmaps[0] = bitmap;

            // 显示到一个ImageView上面
            sweepIV.setImageBitmap(addLogo(bitmaps, startW, starH));
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    // 添加logo
    public Bitmap addLogo(Bitmap[] bitmaps, int w, int h) {

        Bitmap newBitmap = Bitmap.createBitmap(bitmaps[0].getWidth(),
                bitmaps[0].getHeight(), Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(newBitmap);

        for (int i = 0; i < bitmaps.length; i++) {
            if (i == 0) {
                cv.drawBitmap(bitmaps[0], 0, 0, null);
            } else {
                cv.drawBitmap(bitmaps[i], w, h, null);
            }
            cv.save(Canvas.ALL_SAVE_FLAG);
            cv.restore();
        }
        return newBitmap;
    }

    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
