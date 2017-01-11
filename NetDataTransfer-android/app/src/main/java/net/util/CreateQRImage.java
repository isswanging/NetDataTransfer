package net.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import net.app.netdatatransfer.R;
import net.log.Logger;

import java.util.Hashtable;

public class CreateQRImage {
    private final String TAG = "QRImage";

    private ImageView sweepIV;
    private Context context;
    private int QR_WIDTH, QR_HEIGHT;
    private int imageW, imageH;

    public CreateQRImage(String uri, ImageView img, Context context) {
        sweepIV = img;
        this.context = context;
        QR_WIDTH = context.getResources().getDimensionPixelSize(R.dimen.qr_size);
        QR_HEIGHT = context.getResources().getDimensionPixelSize(R.dimen.qr_size);
        Logger.info(TAG, "QR_WIDTH====" + QR_WIDTH);
        createQRImage(uri);
    }

    // 要转换的地址或字符串,可以是中文
    private void createQRImage(String url) {
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

            Hashtable<EncodeHintType, Object> hints = new Hashtable();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            hints.put(EncodeHintType.MARGIN, 0);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
            // 图像数据转换，使用了矩阵转换
            BitMatrix bitMatrix = new QRCodeWriter().encode(url,
                    BarcodeFormat.QR_CODE, QR_WIDTH, QR_HEIGHT, hints);
            int[] pixels = new int[QR_WIDTH * QR_HEIGHT];
            // 下面这里按照二维码的算法，逐个生成二维码的图片，
            // 两个for循环是图片横列扫描的结果
            for (int y = 0; y < QR_HEIGHT; y++) {
                for (int x = 0; x < QR_WIDTH; x++) {
                    if (bitMatrix.get(x, y)) {
                        pixels[y * QR_WIDTH + x] = 0x00000000;
                    } else {
                        pixels[y * QR_WIDTH + x] = 0xffffffff;
                    }
                }
            }
            // 生成二维码图片的格式，使用ARGB_8888
            Bitmap bitmap = Bitmap.createBitmap(QR_WIDTH, QR_HEIGHT,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, QR_WIDTH, 0, 0, QR_WIDTH, QR_HEIGHT);

            Bitmap bitmap2 = ((BitmapDrawable) context.getResources().getDrawable(R.drawable.qc_bg)).getBitmap();
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.LIGHTEN));
            canvas.drawBitmap(bitmap2, 0, 0, paint);
            bitmaps[0] = bitmap;

            // 显示到一个ImageView上面
            sweepIV.setImageBitmap(addLogo(bitmaps, startW, starH));
            bitmap2.recycle();
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    // 添加logo
    public Bitmap addLogo(Bitmap[] bitmaps, int w, int h) {
        Bitmap newBitmap = Bitmap.createBitmap(QR_WIDTH, QR_HEIGHT, Bitmap.Config.ARGB_8888);
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
}
