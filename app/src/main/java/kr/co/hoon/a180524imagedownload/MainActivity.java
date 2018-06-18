package kr.co.hoon.a180524imagedownload;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    ImageView imageView;

    Handler drawHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            // 저장된 Bitmap 가져오기
            Bitmap bitmap = (Bitmap)msg.obj;
            // 이미지 출력
            imageView.setImageBitmap(bitmap);
        }
    };

    class DrawThread extends Thread {
        @Override
        public void run() {
            try{
                // 다운로드 받을 이미지 주소
                // 한글이 있는 경우는 한글부분을 URLEncoder.encode("한글", "UTF-8")로 변환
                // 한글부분만 바꾸지 않고 모두 바꾸게 되면 / 같은 특수문자도 바뀌기 때문에 한글 부분만 바꿔야함
                String addr = "http://jjalbang.today/jjOB.jpg";
                // URL 만들기
                URL url = new URL(addr);
                // 이미지 내용을 읽을 수 있는 스트림
                InputStream is = url.openStream();
                // 비트맵으로 변환
                Bitmap bitmap = BitmapFactory.decodeStream(is);

                // 핸들러에게 bitmap 전달
                Message msg = new Message();
                msg.obj = bitmap;
                drawHandler.sendMessage(msg);
            }catch(Exception e){
                Log.e("예외", e.getMessage());
            }
        }
    }

    // 이미지파일의 내용을 이미지뷰에 출력하는 핸들러
    Handler saveHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            // 안드로이드에서 데이터가 저장되는 절대경로
            String path = Environment.getDataDirectory().getAbsolutePath();
            // 현재 애플리케이션의 디렉토리를 추가하고 파일 경로 추가
            path += "/data/kr.co.hoon.a180524imagedownload/files/" + (String)msg.obj;
            // 파일의 내용을 이미지뷰에 출력
            imageView.setImageBitmap(BitmapFactory.decodeFile(path));
        }
    };

    class SaveThread extends Thread {
        // 다운로드 받을 경로
        String addr;
        // 파일이름
        String filename;

        // 생성자
        public SaveThread(String addr, String filename){
            this.addr = addr;
            this.filename = filename;
        }

        @Override
        public void run() {
            try{
                URL url = new URL(addr);
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                // 다운로드 받을 크기
                int len = conn.getContentLength();
                // 데이터를 저장할 바이트 배열
                byte[] raster = new byte[len];

                // 바이트단위로 읽을 스트림
                InputStream is = conn.getInputStream();

                // 데이터를 저장할 파일스트림
                FileOutputStream fos = openFileOutput(filename, 0);

                while(true){
                    int read = is.read(raster);
                    if(read <= 0){
                        break;
                    }
                    // raster 배열에 처음부터 읽은 개수만큼
                    fos.write(raster, 0, read);
                }
                is.close();
                fos.close();
                conn.disconnect();
            }catch(Exception e){
                Log.e("예외", e.getMessage());
            }

            Message msg = new Message();
            msg.obj = filename;
            saveHandler.sendMessage(msg);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = (ImageView)findViewById(R.id.imageView);
        Button draw = (Button)findViewById(R.id.draw);
        Button save = (Button)findViewById(R.id.save);

        draw.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DrawThread th = new DrawThread();
                th.start();
            }
        });

        save.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                // 다운로드 받을 파일
                String addr = "http://image.sportsseoul.com/2016/07/28/news/20160728144526_5.jpg";
//                String filename = addr.split("/")[addr.length()];
                String filename = addr.substring(addr.lastIndexOf("/")+1);

                // 파일존재여부 확인을 위해 파일의 절대경로
                String path = Environment.getDataDirectory().getAbsolutePath();
                path += "/data/kr.co.hoon.a180524imagedownload/files/" + filename;
                // file 객체 - 존재여부, 업데이트 날짜 등을 알 수 있음
                File f = new File(path);
                if(f.exists()){
                    Toast.makeText(MainActivity.this, "파일이 이미 존재합니다.", Toast.LENGTH_SHORT).show();
                    imageView.setImageBitmap(BitmapFactory.decodeFile(path));
                }else{
                    Toast.makeText(MainActivity.this, "파일이 존재하지 않습니다.", Toast.LENGTH_SHORT).show();
                    // 다운로드
                    SaveThread th = new SaveThread(addr, filename);
                    th.start();
                }
            }
        });
    }
}
