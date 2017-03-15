package cn.yunyunhei.wuhang.pickviewdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.weigan.loopview.LoopView;
import com.weigan.loopview.bean.Bean;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    LoopView test_loopview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        test_loopview = (LoopView) findViewById(R.id.test_loopview);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.drawable.ic_stat_name);

        ArrayList<Bean> list = new ArrayList<>();
        Bean bean;
        for (int i = 0; i < 5; i++) {
            bean = new Bean();
            bean.setFirtLine("first : "+i);
            bean.setSecondLine("second : "+i);
            bean.setBitmap(bitmap);
            list.add(bean);
        }

        test_loopview.setItems(list);
        test_loopview.setNotLoop();
//        test_loopview.setItemsVisibleCount(5);

    }
}
