package com.tzy.toast;

import android.app.Activity;
import android.os.Bundle;

import com.pdf.sf.toast.R;

/**
 * Created by sf on 2016/12/30.
 */
public class SecondActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_second);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MoaToast.makeText(this,"ceahsdiasdijasfddas",MoaToast.LENGTH_SHORT).show();
    }


}
