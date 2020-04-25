package my.test.apt_pro;

import android.view.View;

/**
 * Created by YiVjay
 * on 2020/4/25
 */
public abstract class ReOnClickListener implements View.OnClickListener{
    @Override
    public void onClick(View v) {
        doClick(v);

    }

    public abstract void doClick(View v);
}