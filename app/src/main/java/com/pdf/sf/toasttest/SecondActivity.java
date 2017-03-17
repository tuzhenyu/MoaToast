package com.pdf.sf.toasttest;

import android.app.Activity;
import android.os.Bundle;

/**
 * Created by sf on 2016/12/30.
 */
public class SecondActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MoaToast.makeText(this,"ceahsdiasdijasfddas",MoaToast.LENGTH_SHORT).show();
    }


}
