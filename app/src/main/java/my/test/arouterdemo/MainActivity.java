package my.test.arouterdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import my.test.annotation.BindView;
import my.test.annotation.OnClick;
import my.test.apt_pro.Butterknife;

/**
 * @author YiVjay
 */
public class MainActivity extends AppCompatActivity {

    @BindView(R.id.tv)
    TextView    tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Butterknife.bind(this);
    }

    @OnClick({R.id.tv})
    public void Click(View v){
        Toast.makeText(MainActivity.this,"点击",Toast.LENGTH_SHORT).show();

    }
}
